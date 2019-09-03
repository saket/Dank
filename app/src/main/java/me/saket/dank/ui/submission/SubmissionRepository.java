package me.saket.dank.ui.submission;

import androidx.annotation.CheckResult;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxbinding2.internal.Notification;
import com.nytimes.android.external.cache3.Cache;
import com.nytimes.android.external.cache3.CacheBuilder;
import com.squareup.moshi.Moshi;
import com.squareup.sqlbrite2.BriteDatabase;

import net.dean.jraw.models.CommentSort;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.RootCommentNode;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import me.saket.dank.BuildConfig;
import me.saket.dank.data.AppDatabase;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.FullNameType;
import me.saket.dank.data.PaginationAnchor;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.reddit.Reddit;
import me.saket.dank.reply.ReplyRepository;
import me.saket.dank.ui.submission.AuditedCommentSort.SelectedBy;
import me.saket.dank.ui.subreddit.SubmissionPaginationResult;
import me.saket.dank.ui.subreddit.SubredditSearchResult;
import me.saket.dank.ui.subscriptions.SubscriptionRepository;
import me.saket.dank.utils.Arrays2;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.Pair;
import me.saket.dank.vote.VotingManager;
import me.saket.dank.walkthrough.SyntheticData;
import me.saket.dank.walkthrough.SyntheticSubmissionAndComments;
import timber.log.Timber;

import static io.reactivex.schedulers.Schedulers.io;

@Singleton
public class SubmissionRepository {

  private final Lazy<Moshi> moshi;
  @Deprecated private final Lazy<BriteDatabase> database;
  private final Lazy<AppDatabase> roomDatabase;
  private final Lazy<Reddit> reddit;
  private final Lazy<VotingManager> votingManager;
  private final Lazy<ErrorResolver> errorResolver;
  private final Lazy<SubscriptionRepository> subscriptionRepository;
  private final Lazy<SyntheticData> syntheticData;
  private final Lazy<ReplyRepository> replyRepository;

  private Cache<DankSubmissionRequest, CachedSubmissionAndComments> inMemoryCache;

  @Inject
  public SubmissionRepository(
      Lazy<Moshi> moshi,
      Lazy<BriteDatabase> briteDatabase,
      Lazy<AppDatabase> roomDatabase,
      Lazy<Reddit> reddit,
      Lazy<VotingManager> votingManager,
      Lazy<ErrorResolver> errorResolver,
      Lazy<SubscriptionRepository> subscriptionRepository,
      Lazy<ReplyRepository> replyRepository,
      Lazy<SyntheticData> syntheticData)
  {
    this.database = briteDatabase;
    this.moshi = moshi;
    this.roomDatabase = roomDatabase;
    this.reddit = reddit;
    this.votingManager = votingManager;
    this.errorResolver = errorResolver;
    this.subscriptionRepository = subscriptionRepository;
    this.syntheticData = syntheticData;
    this.replyRepository = replyRepository;

    inMemoryCache = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .maximumSize(100)
        .build();
  }

// ======== SUBMISSION WITH COMMENTS ======== //

  /**
   * Get from DB or from the network if not present in DB.
   *
   * @return Pair of an optionally-updated submission request in case remote suggested a
   * different sort for comments and the submission object with comments.
   */
  @CheckResult
  public Observable<Pair<DankSubmissionRequest, SubmissionAndComments>> submissionWithComments(DankSubmissionRequest oldRequest) {
    if (oldRequest.id().equalsIgnoreCase(SyntheticData.SUBMISSION_ID_FOR_GESTURE_WALKTHROUGH)) {
      //Timber.i("Returning from Synthetic.");
      return syntheticSubmissionForGesturesWalkthrough()
          .map(syntheticSubmissionData -> {
            //noinspection ConstantConditions
            DankSubmissionRequest updatedRequest = oldRequest.toBuilder()
                .commentSort(syntheticSubmissionData.getSubmission().getSuggestedSort(), SelectedBy.DEFAULT)
                .build();
            return Pair.create(updatedRequest, syntheticSubmissionData.toNonSynthetic());
          })
          .toObservable();
    }

    Observable<Pair<DankSubmissionRequest, CachedSubmissionAndComments>> dbStream = getFromDbOrFetchSubmissionWithComments(oldRequest)
        .take(1)
        .flatMap(submissionWithComments -> {
          // The aim is to always load comments in the sort mode suggested by a subreddit. In case we
          // load with the wrong sort (possibly because the submission's details were unknown), reload
          // comments using the suggested sort.
          CommentSort suggestedSort = submissionWithComments.getSubmission().getSuggestedSort();
          Boolean useSuggestedSort = suggestedSort != null && oldRequest.commentSort().canOverrideWithSuggestedSort();

          if (useSuggestedSort) {
            //Timber.i("Different sort.");
            DankSubmissionRequest newRequest = oldRequest.toBuilder()
                .commentSort(suggestedSort, SelectedBy.SUBMISSION_SUGGESTED)
                .build();

            return getFromDbOrFetchSubmissionWithComments(newRequest)
                .map(submissions -> Pair.create(newRequest, submissions));

          } else {
            //Timber.i("Returning from DB with the same sort again");
            // We're calling getOrFetch() again to receive a refreshing Observable.
            return getFromDbOrFetchSubmissionWithComments(oldRequest)
                .startWith(submissionWithComments)
                .map(submissions -> Pair.create(oldRequest, submissions))
                //.compose(RxUtils.doOnceOnNext(o -> Timber.i("Returned from memory")))
                ;
          }
        });

    return dbStream
        .doOnNext(pair -> inMemoryCache.put(pair.first(), pair.second()))
        .startWith(Observable.create(emitter -> {
          CachedSubmissionAndComments inMemoryValue = inMemoryCache.getIfPresent(oldRequest);
          if (inMemoryValue != null) {
            emitter.onNext(Pair.create(oldRequest, inMemoryValue));
          }
          emitter.onComplete();
        }))
        .distinctUntilChanged()
        .map(pair -> {
          DankSubmissionRequest request = pair.first();
          SubmissionAndComments submissionData = SubmissionAndComments.Companion.from(pair.second());
          return Pair.create(request, submissionData);
        });
  }

  /**
   * Get from DB or from the network if not present in DB.
   */
  @CheckResult
  private Observable<CachedSubmissionAndComments> getFromDbOrFetchSubmissionWithComments(DankSubmissionRequest request) {
    // This stream is intentionally not shared. I don't know why, but the network call was blocking the DB stream.
    Observable<List<CachedSubmissionAndComments>> dbStream = roomDatabase.get()
        .submissionDao()
        .submissionWithComments(request.id(), request)
        .toObservable();

    Completable refreshCompletable = dbStream
        .observeOn(io())
        .map(Arrays2::firstOrEmpty)
        .filter(optionalSubmission -> optionalSubmission.isEmpty() || optionalSubmission.get().comments().isEmpty())
        .flatMapCompletable(o -> {
          Single<RootCommentNode> cachedNetworkStream = reddit.get().submissions()
              .fetch(request)
              .cache();

          Completable saveCompletable = cachedNetworkStream
              .map(node -> {
                Submission submission = node.getSubject();
                CachedSubmissionComments cachedComments = new CachedSubmissionComments(
                    submission.getId(),
                    node.getChildren(),
                    request,
                    System.currentTimeMillis());
                CachedSubmission cachedSubmission = new CachedSubmission(
                    submission.getId(),
                    submission,
                    submission.getSubreddit(),
                    System.currentTimeMillis());
                return Pair.create(cachedSubmission, cachedComments);
              })
              .flatMapCompletable(this::saveSubmissionData);

          Completable removeStaleSyncedLocalReplies = cachedNetworkStream
              .map(node -> node.getSubject())
              .map(ParentThread::of)
              .flatMapCompletable(replyRepository.get()::removeSyncPendingPostedReplies);

          return saveCompletable.mergeWith(removeStaleSyncedLocalReplies);
        });

    return dbStream
        .flatMap(dbItems -> dbItems.isEmpty() ? Observable.empty() : Observable.just(dbItems.get(0)))
        .mergeWith(refreshCompletable.toObservable());
  }

  private Completable saveSubmissionData(Pair<CachedSubmission, CachedSubmissionComments> submissionData) {
    return Completable.fromAction(() -> {
      CachedSubmission cachedSubmission = submissionData.first();
      CachedSubmissionComments cachedSubmissionComments = submissionData.second();

      roomDatabase.get().submissionDao().saveSubmission(cachedSubmission);
      roomDatabase.get().submissionDao().saveComments(cachedSubmissionComments);
    });
  }

  private Completable saveSubmissionComments(SubmissionAndComments submissionData, DankSubmissionRequest request) {
    return Completable.fromAction(() -> {
      //noinspection ConstantConditions
      CachedSubmissionComments cachedSubmissionComments = new CachedSubmissionComments(
          submissionData.getSubmission().getId(),
          submissionData.getComments().get().getChildren(),
          request,
          System.currentTimeMillis());
      roomDatabase.get().submissionDao().saveComments(cachedSubmissionComments);
    });
  }

  @CheckResult
  public Completable loadAndSaveMoreComments(SubmissionAndComments submissionData, DankSubmissionRequest request, CommentNode commentNode) {
    if (!commentNode.getSettings().getSubmissionId().equals(submissionData.getSubmission().getId())) {
      throw new AssertionError("CommentNode does not belong to this submission");
    }

    //noinspection unchecked
    return ((Single<SubmissionAndComments>) reddit.get().submissions().fetchMoreComments(submissionData, commentNode))
        .flatMapCompletable(updatedSubmissionData -> saveSubmissionComments(updatedSubmissionData, request));
  }

  public Completable clearCachedSubmissionComments(DankSubmissionRequest request) {
    return Completable.fromAction(() -> {
      inMemoryCache.invalidate(request);
      roomDatabase.get().submissionDao().deleteComments(request);
    });
  }

  @CheckResult
  public Completable clearAllCachedSubmissionComments() {
    if (!BuildConfig.DEBUG) {
      throw new AssertionError();
    }
    return Completable.fromAction(() -> {
      inMemoryCache.invalidateAll();
      roomDatabase.get().submissionDao().deleteAllComments();
    });
  }

  @CheckResult
  public Single<SyntheticSubmissionAndComments> syntheticSubmissionForGesturesWalkthrough() {
    return Single.just(new SyntheticSubmissionAndComments());
  }

// ======== SUBMISSION LIST (W/O COMMENTS) ======== //

  @CheckResult
  public Observable<List<Submission>> submissions(CachedSubmissionFolder folder) {
    return roomDatabase.get()
        .submissionDao()
        .submissionsInFolderAsc(folder.subredditName(), folder.sortingAndTimePeriod())
        .toObservable();
  }

  /**
   * @return Operates on the main thread.
   */
  @CheckResult
  public Observable<SubmissionPaginationResult> loadAndSaveMoreSubmissions(CachedSubmissionFolder folder) {
    return lastPaginationAnchor(folder)
        //.doOnSuccess(anchor -> Timber.i("anchor: %s", anchor))
        .flatMapCompletable(anchor -> Single
            .fromCallable(() -> {
              PaginationAnchor nextAnchor = anchor;
              int savedSubmissionCount = 0;

              while (true) {
                FetchResult fetchResult = fetchSubmissionsFromRemoteWithAnchor(folder, nextAnchor);
                votingManager.get().removePendingVotesForFetchedSubmissions(fetchResult.fetchedSubmissions()).subscribe();
                //Timber.i("Found %s submissions on remote", fetchResult.fetchedSubmissions().size());

                SaveResult saveResult = saveSubmissions(folder, fetchResult.fetchedSubmissions());
                savedSubmissionCount += saveResult.savedItems().size();

                if (!fetchResult.hasMoreItems() || savedSubmissionCount > 10) {
                  //Timber.i("Breaking early");
                  break;
                }

                //Timber.i("%s distinct items not enough", distinctNewItems.size());
                Submission lastFetchedSubmission = fetchResult.fetchedSubmissions().get(fetchResult.fetchedSubmissions().size() - 1);
                nextAnchor = PaginationAnchor.create(lastFetchedSubmission.getFullName());
              }
              //Timber.i("Fetched %s submissions", distinctNewItems.size());
              return savedSubmissionCount;
            })
            .toCompletable()
            .onErrorResumeNext(error -> {
              SubredditSearchResult result = reddit.get().subreddits().parseSubmissionPaginationError(error);
              switch (result.type()) {
                case ERROR_PRIVATE:
                  return Completable.error(new PrivateSubredditException());

                case ERROR_NOT_FOUND:
                  return Completable.error(new SubredditNotFoundException());

                case ERROR_UNKNOWN:
                  return Completable.error(error);

                default:
                  throw new AssertionError("Unknown result: " + result);
              }
            })
            .retryWhen(errors -> errors.flatMap(error -> {
              Throwable actualError = errorResolver.get().findActualCause(error);

              if (actualError instanceof InterruptedIOException) {
                // I don't know why, but this chain gets occasionally gets interrupted randomly.
                //Timber.w("Retrying on thread interruption");
                return Flowable.just((Object) Notification.INSTANCE);
              } else {
                //error.printStackTrace();
                return Flowable.error(error);
              }
            }))
        )
        .toSingleDefault(SubmissionPaginationResult.idle())
        .toObservable()
        .doOnError(e -> {
          if (!(e instanceof PrivateSubredditException || e instanceof SubredditNotFoundException)) {
            ResolvedError resolvedError = errorResolver.get().resolve(e);
            resolvedError.ifUnknown(() -> Timber.e(e, "Couldn't fetch submissions for %s", folder));
          }
        })
        .onErrorReturn(error -> SubmissionPaginationResult.failed(error))
        .startWith(SubmissionPaginationResult.inFlight());
  }

  /**
   * Create a PaginationAnchor from the last cached submission under <var>folder</var>.
   */
  @CheckResult
  private Single<PaginationAnchor> lastPaginationAnchor(CachedSubmissionFolder folder) {
    return Single.fromCallable(() -> {
      CachedSubmissionId2 lastId = roomDatabase.get().submissionDao().lastSubmissionId(folder.subredditName(), folder.sortingAndTimePeriod());
      return lastId == null
          ? PaginationAnchor.createEmpty()
          : PaginationAnchor.create(FullNameType.SUBMISSION.prefix() + lastId.getId());
    });
  }

  @CheckResult
  private FetchResult fetchSubmissionsFromRemoteWithAnchor(CachedSubmissionFolder folder, PaginationAnchor anchor) {
    boolean isFrontpage = subscriptionRepository.get().isFrontpage(folder.subredditName());

    Iterator<Listing<Submission>> subredditPaginator;
    if (anchor.isEmpty()) {
      subredditPaginator = reddit.get().subreddits().submissions(
          folder.subredditName(),
          isFrontpage,
          folder.sortingAndTimePeriod().sortOrder(),
          folder.sortingAndTimePeriod().timePeriod());

    } else {
      subredditPaginator = reddit.get().subreddits().submissions(
          folder.subredditName(),
          isFrontpage,
          folder.sortingAndTimePeriod().sortOrder(),
          folder.sortingAndTimePeriod().timePeriod(),
          anchor.fullName());
    }

    Listing<Submission> submissions = subredditPaginator.next();
    return FetchResult.create(submissions, subredditPaginator.hasNext());
  }

  /**
   * Note: This will ignore duplicates.
   *
   * @return Saved submissions with duplicates ignored.
   */
  @CheckResult
  private SaveResult saveSubmissions(CachedSubmissionFolder folder, List<Submission> submissionsToSave) {
    ArrayList<CachedSubmissionId2> cachedSubmissionIds = new ArrayList<>(submissionsToSave.size());
    ArrayList<CachedSubmission> cachedSubmissions = new ArrayList<>(submissionsToSave.size());

    for (int i = 0; i < submissionsToSave.size(); i++) {
      // Reddit sends submissions according to their sorting order. So they may or may not be
      // sorted by their creation time. However, we want to store their download time so that
      // they can be fetched in the same order (because SQLite doesn't guarantee preservation
      // of insertion order). Adding the index to avoid duplicate times.
      long saveTimeMillis = System.currentTimeMillis() + i;
      Submission submission = submissionsToSave.get(i);

      // Warning: get the subreddit name from the folder and not the submissions or else
      // "Frontpage", "Popular", etc. will never get anything.
      String subredditName = folder.subredditName();

      CachedSubmissionId2 cachedSubmissionId = new CachedSubmissionId2(
          submission.getId(),
          subredditName,
          folder.sortingAndTimePeriod(),
          saveTimeMillis);

      CachedSubmission cachedSubmission = new CachedSubmission(
          submission.getId(),
          submission,
          submission.getSubreddit(),
          saveTimeMillis);

      cachedSubmissionIds.add(cachedSubmissionId);
      cachedSubmissions.add(cachedSubmission);
    }

    List<CachedSubmission> savedSubmissions = new ArrayList<>(submissionsToSave.size());

    roomDatabase.get().runInTransaction(() -> {
      for (int i = 0; i < cachedSubmissionIds.size(); i++) {
        CachedSubmissionId2 cachedSubmissionId = cachedSubmissionIds.get(i);
        CachedSubmission cachedSubmission = cachedSubmissions.get(i);

        // Again, since Reddit does not send submissions sorted by time,
        // it's possible to receive duplicate submissions. Ignore them.
        long insertedRowId = roomDatabase.get().submissionDao().saveSubmissionIdIfNew(cachedSubmissionId);
        if (insertedRowId != -1) {
          roomDatabase.get().submissionDao().saveSubmission(cachedSubmission);
          savedSubmissions.add(cachedSubmission);
        }
      }
    });
    //Timber.i("Saved %d items in: %sms", submissionsToSave.size(), (System.currentTimeMillis() - startTime));

    return SaveResult.create(Collections.unmodifiableList(savedSubmissions));
  }

  public Completable clearCachedSubmissionLists() {
    return Completable.fromAction(() -> roomDatabase.get().submissionDao().deleteAllSubmissionIds());
  }

  public Completable clearCachedSubmissionLists(String subredditName) {
    return Completable.fromAction(() -> roomDatabase.get().submissionDao().deleteSubmissionIdsInSubredit(subredditName));
  }

  /**
   * Recycle all items that were saved X millis before now. If <var>durationFromNow</var> is 7 and
   * timeUnit is DAYS, all items saved before 7 days ago will be removed.
   */
  @CheckResult
  public Single<Integer> recycleAllCachedBefore(int durationFromNow, TimeUnit durationTimeUnit) {
    long daysBeforeNow = durationTimeUnit.toDays(durationFromNow);
    LocalDateTime dateTimeBeforeNow = LocalDateTime.now(ZoneId.of("UTC")).minusDays(daysBeforeNow);
    long millisBeforeNow = dateTimeBeforeNow.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();

    Completable logCompletable;

    if (BuildConfig.DEBUG) {
      logCompletable = roomDatabase.get()
          .submissionDao()
          .countOfSubmissionWithComments(millisBeforeNow)
          .firstOrError()
          .flatMapCompletable(rowsToDelete ->
              Completable.fromAction(() -> {
                Timber.i("Now time: %s", System.currentTimeMillis());

                Timber.i("Recycling rows before %s (%s): %s", millisBeforeNow, dateTimeBeforeNow, rowsToDelete.size());
                for (CachedSubmissionComments row : rowsToDelete) {
                  Timber.i("%s", row.getSubmissionId());
                }
              })
          );

    } else {
      logCompletable = Completable.complete();
    }

    return logCompletable
        .andThen(Single.fromCallable(() -> roomDatabase.get().submissionDao().deleteAllSubmissionRelatedRows(millisBeforeNow)));
  }

  @AutoValue
  abstract static class SaveResult {
    public abstract List<Object> savedItems();

    public static SaveResult create(List<Object> savedItems) {
      return new AutoValue_SubmissionRepository_SaveResult(savedItems);
    }
  }

  @AutoValue
  abstract static class FetchResult {

    public abstract List<Submission> fetchedSubmissions();

    /**
     * Whether more submissions can be fetched after this.
     */
    public abstract boolean hasMoreItems();

    public static FetchResult create(List<Submission> fetchedItems, boolean hasMoreItems) {
      return new AutoValue_SubmissionRepository_FetchResult(fetchedItems, hasMoreItems);
    }
  }
}
