package me.saket.dank.ui.submission;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.CheckResult;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxbinding2.internal.Notification;
import com.nytimes.android.external.store3.base.Persister;
import com.nytimes.android.external.store3.base.impl.Store;
import com.nytimes.android.external.store3.base.impl.StoreBuilder;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.sqlbrite2.BriteDatabase;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.CommentSort;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.PaginationAnchor;
import me.saket.dank.data.PostedOrInFlightContribution;
import me.saket.dank.data.SubredditSubscriptionManager;
import me.saket.dank.data.VotingManager;
import me.saket.dank.ui.subreddits.NetworkCallStatus;
import me.saket.dank.utils.Commons;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.Pair;
import timber.log.Timber;

@Singleton
public class SubmissionRepository {

  private final BriteDatabase database;
  private final Moshi moshi;
  private final DankRedditClient dankRedditClient;
  private final VotingManager votingManager;
  private final Set<String> savedSubmissionIds = new HashSet<>();
  private final ErrorResolver errorResolver;
  private final SubredditSubscriptionManager subscriptionManager;
  private final ReplyRepository replyRepository;
  private Store<CachedSubmissionWithComments, DankSubmissionRequest> submissionWithCommentsStore;

  @Inject
  public SubmissionRepository(BriteDatabase briteDatabase, Moshi moshi, DankRedditClient dankRedditClient, VotingManager votingManager,
      ErrorResolver errorResolver, SubredditSubscriptionManager subscriptionManager, ReplyRepository replyRepository)
  {
    this.database = briteDatabase;
    this.moshi = moshi;
    this.dankRedditClient = dankRedditClient;
    this.votingManager = votingManager;
    this.errorResolver = errorResolver;
    this.subscriptionManager = subscriptionManager;
    this.replyRepository = replyRepository;
  }

// ======== SUBMISSION WITH COMMENTS ======== //

  /**
   * Get from DB or from the network if not present in DB.
   *
   * @return Pair of an optionally-updated submission request in case remote suggested a
   * different sort for comments and the submission object with comments.
   */
  @CheckResult
  public Observable<Pair<DankSubmissionRequest, Submission>> submissionWithComments(DankSubmissionRequest oldSubmissionRequest) {
    Observable<Pair<DankSubmissionRequest, CachedSubmissionWithComments>> stream = getOrFetchSubmissionWithComments(oldSubmissionRequest)
        .take(1)
        .flatMap(submissionWithComments -> {
          // The aim is to always load comments in the sort mode suggested by a subreddit. In case we
          // load with the wrong sort (possibly because the submission's details were unknown), reload
          // comments using the suggested sort.
          CommentSort suggestedSort = submissionWithComments.submission().getSuggestedSort();

          if (suggestedSort != null && suggestedSort != oldSubmissionRequest.commentSort()) {
            DankSubmissionRequest newRequest = oldSubmissionRequest.toBuilder()
                .commentSort(suggestedSort)
                .build();
            return getOrFetchSubmissionWithComments(newRequest)
                .map(submissions -> Pair.create(newRequest, submissions))
                .doAfterNext(o -> {
                  Timber.i("Clearing old memory cache key because of submission request mismatch.");
                  submissionWithCommentsStore.clear(oldSubmissionRequest);
                })
                ;

          } else {
            // This should return immediately because the store has an in-memory cache.
            return getOrFetchSubmissionWithComments(oldSubmissionRequest)
                // There is an odd behavior where the cache store gets stuck and doesn't respond,
                // leading to the comments never showing up.
                .timeout(Observable.timer(300, TimeUnit.MILLISECONDS), o -> Observable.timer(100, TimeUnit.DAYS))
                .retry(10, error -> {
                  if (error instanceof TimeoutException) {
                    Timber.w("Retrying because memory store isn't responding.");
                    return true;
                  } else {
                    return false;
                  }
                })
                .skip(1)
                .startWith(submissionWithComments)
                .map(submissions -> Pair.create(oldSubmissionRequest, submissions));
          }
        });

    Observable<Pair<DankSubmissionRequest, CachedSubmissionWithComments>> sharedStream = stream.share();

    // Dank unfortunately has two storage locations for submissions:
    // - one for submission list
    // - one for submission with comments.
    //
    // I need to figure out to use a single storage for both, but for now keeping both of
    // them in sync is the only option. When reading submissions with comments, we'll check
    // if an updated copy is available in the submission list table. We do the reverse when
    // saving a submission with comments. See saveSubmissionWithAndWithoutComments().
    Completable updateCompletable = sharedStream
        .take(1)
        .flatMapCompletable(pair -> {
          DankSubmissionRequest updatedSubmissionRequest = pair.first();
          CachedSubmissionWithComments possiblyStaleSubWithComments = pair.second();

          return database
              .createQuery(
                  CachedSubmissionWithoutComments.TABLE_NAME,
                  CachedSubmissionWithoutComments.SELECT_BY_FULLNAME_AND_SAVE_TIME_NEWER_THAN,
                  possiblyStaleSubWithComments.submission().getFullName(),
                  String.valueOf(possiblyStaleSubWithComments.updateTimeMillis())
              )
              .mapToList(cursor -> CachedSubmissionWithoutComments.createFromCursor(cursor, moshi))
              .take(1)
              .flatMap(items -> items.isEmpty() ? Observable.empty() : Observable.just(items.get(0)))
              .map(optionalNewerSubWithoutComments -> CachedSubmissionWithComments.create(
                  updatedSubmissionRequest,
                  new Submission(
                      optionalNewerSubWithoutComments.submission().getDataNode(),
                      possiblyStaleSubWithComments.submission().getComments()
                  ),
                  optionalNewerSubWithoutComments.saveTimeMillis()
              ))
              .flatMapCompletable(newCachedSub -> Completable.fromAction(() ->
                  database.insert(CachedSubmissionWithComments.TABLE_NAME, newCachedSub.toContentValues(moshi), SQLiteDatabase.CONFLICT_REPLACE)
              ))
              // This will trigger another emission from the cache store.
              .andThen(Completable.fromAction(() -> submissionWithCommentsStore.clear(updatedSubmissionRequest)));
        });

    updateCompletable
        .subscribeOn(Schedulers.io())
        .subscribe();

    return sharedStream.map(pair -> Pair.create(pair.first(), pair.second().submission()));
  }

  /**
   * Get from DB or from the network if not present in DB.
   */
  @CheckResult
  private Observable<CachedSubmissionWithComments> getOrFetchSubmissionWithComments(DankSubmissionRequest submissionRequest) {
    if (submissionWithCommentsStore == null) {
      submissionWithCommentsStore = StoreBuilder.<DankSubmissionRequest, CachedSubmissionWithComments>key()
          .fetcher(request -> dankRedditClient
              .submission(request)
              .doOnSuccess(subWithComments -> replyRepository
                  .removeSyncPendingPostedReplies(ParentThread.of(subWithComments))
                  .blockingAwait())
              .map(subWithComments -> CachedSubmissionWithComments.create(request, subWithComments, System.currentTimeMillis()))
          )
          .persister(new Persister<CachedSubmissionWithComments, DankSubmissionRequest>() {
            @Nonnull
            @Override
            public Maybe<CachedSubmissionWithComments> read(DankSubmissionRequest submissionRequest) {
              String requestJson = moshi.adapter(DankSubmissionRequest.class).toJson(submissionRequest);

              return database.createQuery(CachedSubmissionWithComments.TABLE_NAME, CachedSubmissionWithComments.SELECT_BY_REQUEST_JSON, requestJson)
                  .mapToList(CachedSubmissionWithComments.cursorMapper(moshi))
                  .firstElement()
                  .flatMap(cachedSubmissions -> {
                    if (cachedSubmissions.isEmpty()) {
                      return Maybe.empty();
                    } else {
                      return Maybe.just(cachedSubmissions.get(0));
                    }
                  });
            }

            @Nonnull
            @Override
            public Single<Boolean> write(DankSubmissionRequest submissionRequest, CachedSubmissionWithComments cachedSubmission) {
              return saveSubmissionWithAndWithoutComments(cachedSubmission).toSingleDefault(true);
            }
          })
          .open();
    }

    return submissionWithCommentsStore.getRefreshing(submissionRequest);
  }

  @CheckResult
  private Completable saveSubmissionWithAndWithoutComments(CachedSubmissionWithComments cachedSubmission) {
    return Completable.fromAction(() -> {
      //Timber.i("Saving submission with comments: %s", submission.getTitle());

      long saveTimeMillis = System.currentTimeMillis();

      Submission submissionWithoutComments = new Submission(cachedSubmission.submission().getDataNode());
      CachedSubmissionWithoutComments cachedSubmissionWithoutComments = CachedSubmissionWithoutComments.create(
          cachedSubmission.submission().getFullName(),
          submissionWithoutComments,
          submissionWithoutComments.getSubredditName(),
          saveTimeMillis
      );
      JsonAdapter<Submission> submissionJsonAdapter = moshi.adapter(Submission.class);

      try (BriteDatabase.Transaction transaction = database.newTransaction()) {
        database.insert(CachedSubmissionWithComments.TABLE_NAME, cachedSubmission.toContentValues(moshi), SQLiteDatabase.CONFLICT_REPLACE);
        database.insert(CachedSubmissionWithoutComments.TABLE_NAME, cachedSubmissionWithoutComments.toContentValues(submissionJsonAdapter),
            SQLiteDatabase.CONFLICT_REPLACE);
        transaction.markSuccessful();
      }
    });
  }

  @CheckResult
  public Completable loadMoreComments(Submission submission, DankSubmissionRequest submissionRequest, CommentNode commentNode) {
    if (!commentNode.getSubmissionName().equals(submission.getFullName())) {
      throw new AssertionError("CommentNode does not belong to the supplied submission");
    }

    // JRAW inserts the new comments directly inside submission's comment tree, which we
    // do not want because we treat persistence as the single source of truth. So we'll
    // instead re-save the submission to DB and let the UI update itself.
    return dankRedditClient.withAuth(dankRedditClient.loadMoreComments(commentNode))
        .toCompletable()
        .andThen(saveSubmissionWithAndWithoutComments(CachedSubmissionWithComments.create(
            submissionRequest,
            submission,
            System.currentTimeMillis()
        )));
  }

// ======== SUBMISSION LIST (W/O COMMENTS) ======== //

  @CheckResult
  public Observable<List<Submission>> submissions(CachedSubmissionFolder folder) {
    List<String> tablesToListen = Arrays.asList(CachedSubmissionId.TABLE_NAME, CachedSubmissionWithoutComments.TABLE_NAME);
    String sqlQuery = CachedSubmissionList.constructQueryToGetAll(moshi, folder.subredditName(), folder.sortingAndTimePeriod());

    return database.createQuery(tablesToListen, sqlQuery)
        .mapToList(CachedSubmissionList.cursorMapper(moshi.adapter(Submission.class)))
        .map(Commons.toImmutable());
  }

  /**
   * @return Operates on the main thread.
   */
  @CheckResult
  public Observable<NetworkCallStatus> loadAndSaveMoreSubmissions(CachedSubmissionFolder folder) {
    return lastPaginationAnchor(folder)
        //.doOnSuccess(anchor -> Timber.i("anchor: %s", anchor))
        .flatMapCompletable(anchor -> dankRedditClient
            .withAuth(Completable.fromAction(() -> {
              List<Submission> distinctNewItems = new ArrayList<>();
              PaginationAnchor nextAnchor = anchor;

              while (true) {
                FetchResult fetchResult = fetchSubmissionsFromRemoteWithAnchor(folder, nextAnchor);
                votingManager.removePendingVotesForFetchedSubmissions(fetchResult.fetchedSubmissions()).subscribe();
                //Timber.i("Found %s submissions on remote", fetchResult.fetchedSubmissions().size());

                SaveResult saveResult = saveSubmissions(folder, fetchResult.fetchedSubmissions());
                distinctNewItems.addAll(saveResult.savedItems());

                if (!fetchResult.hasMoreItems() || distinctNewItems.size() > 10) {
                  //Timber.i("Breaking early");
                  break;
                }

                //Timber.i("%s distinct items not enough", distinctNewItems.size());
                Submission lastFetchedSubmission = fetchResult.fetchedSubmissions().get(fetchResult.fetchedSubmissions().size() - 1);
                nextAnchor = PaginationAnchor.create(lastFetchedSubmission.getFullName());
              }
              //Timber.i("Fetched %s submissions", distinctNewItems.size());
            }))
            .retryWhen(errors -> errors.flatMap(error -> {
              Throwable actualError = errorResolver.findActualCause(error);

              if (actualError instanceof InterruptedIOException) {
                // I don't know why, but this chain gets occasionally gets interrupted randomly.
                //Timber.w("Retrying on thread interruption");
                return Flowable.just((Object) Notification.INSTANCE);
              } else {
                return Flowable.error(error);
              }
            }))
        )
        .toSingleDefault(NetworkCallStatus.createIdle())
        .toObservable()
        .doOnError(e -> Timber.e("%s", e.getMessage()))
        .onErrorReturn(error -> NetworkCallStatus.createFailed(error))
        .startWith(NetworkCallStatus.createInFlight());
  }

  /**
   * Create a PaginationAnchor from the last cached submission under <var>folder</var>.
   */
  @CheckResult
  private Single<PaginationAnchor> lastPaginationAnchor(CachedSubmissionFolder folder) {
    String sortingAndTimePeriodJson = moshi.adapter(SortingAndTimePeriod.class).toJson(folder.sortingAndTimePeriod());
    String sqlQuery = CachedSubmissionId.constructQueryToGetLastSubmission(folder.subredditName(), sortingAndTimePeriodJson);

    Function<Cursor, PaginationAnchor> paginationAnchorFunction = cursor -> {
      //noinspection CodeBlock2Expr
      return PaginationAnchor.create(CachedSubmissionId.SUBMISSION_FULLNAME_MAPPER.apply(cursor));
    };

    return database
        .createQuery(CachedSubmissionId.TABLE_NAME, sqlQuery)
        .mapToOneOrDefault(paginationAnchorFunction, PaginationAnchor.createEmpty())
        .take(1)
        .firstOrError();
  }

  @CheckResult
  private FetchResult fetchSubmissionsFromRemoteWithAnchor(CachedSubmissionFolder folder, PaginationAnchor anchor) {
    boolean isFrontpage = subscriptionManager.isFrontpage(folder.subredditName());
    SubredditPaginator subredditPaginator = dankRedditClient.subredditPaginator(folder.subredditName(), isFrontpage);
    if (!anchor.isEmpty()) {
      subredditPaginator.setStartAfterThing(anchor.fullName());
    }

    subredditPaginator.setSorting(folder.sortingAndTimePeriod().sortOrder());
    subredditPaginator.setTimePeriod(folder.sortingAndTimePeriod().timePeriod());
    Listing<Submission> submissions = subredditPaginator.next(true);

    return FetchResult.create(submissions, subredditPaginator.hasNext());
  }

  /**
   * Note: This will ignore duplicates.
   *
   * @return Saved submissions with duplicates ignored.
   */
  @CheckResult
  private SaveResult saveSubmissions(CachedSubmissionFolder folder, List<Submission> submissionsToSave) {
    List<Submission> savedSubmissions = new ArrayList<>(submissionsToSave.size());
    JsonAdapter<SortingAndTimePeriod> andTimePeriodJsonAdapter = moshi.adapter(SortingAndTimePeriod.class);
    JsonAdapter<Submission> submissionJsonAdapter = moshi.adapter(Submission.class);

    try (BriteDatabase.Transaction transaction = database.newTransaction()) {
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

        CachedSubmissionId cachedSubmissionId = CachedSubmissionId.create(
            submission.getFullName(),
            subredditName,
            folder.sortingAndTimePeriod(),
            saveTimeMillis
        );

        CachedSubmissionWithoutComments cachedSubmissionWithoutComments = CachedSubmissionWithoutComments.create(
            submission.getFullName(),
            submission,
            submission.getSubredditName(),
            saveTimeMillis
        );

        // Again, since Reddit does not send submissions sorted by time, it's possible to receive
        // duplicate submissions.Ignore them.
        long insertedRowId = database.insert(
            CachedSubmissionId.TABLE_NAME,
            cachedSubmissionId.toContentValues(andTimePeriodJsonAdapter),
            SQLiteDatabase.CONFLICT_IGNORE
        );
        if (insertedRowId != -1) {
          database.insert(
              CachedSubmissionWithoutComments.TABLE_NAME,
              cachedSubmissionWithoutComments.toContentValues(submissionJsonAdapter),
              SQLiteDatabase.CONFLICT_REPLACE /* To handle updated submissions received from remote */
          );
          savedSubmissions.add(submission);
        }
      }

      transaction.markSuccessful();
    }
    //Timber.i("Saved %d items in: %sms", submissionsToSave.size(), (System.currentTimeMillis() - startTime));

    return SaveResult.create(Collections.unmodifiableList(savedSubmissions));
  }

  public Completable clearCachedSubmissionLists() {
    return Completable.fromAction(() -> {
      try (BriteDatabase.Transaction transaction = database.newTransaction()) {
        database.delete(CachedSubmissionId.TABLE_NAME, null);
        database.delete(CachedSubmissionWithoutComments.TABLE_NAME, null);

        transaction.markSuccessful();
      }
    });
  }

  public Completable clearCachedSubmissionLists(String subredditName) {
    return Completable.fromAction(() -> {
      try (BriteDatabase.Transaction transaction = database.newTransaction()) {
        database.delete(CachedSubmissionId.TABLE_NAME, CachedSubmissionId.WHERE_SUBREDDIT_NAME, subredditName);
        database.delete(CachedSubmissionWithoutComments.TABLE_NAME, CachedSubmissionWithoutComments.WHERE_SUBREDDIT_NAME, subredditName);
        transaction.markSuccessful();
      }
    });
  }

  /**
   * Recycle all items that were saved X millis before now. If <var>durationBeforeNow</var> is 7 and
   * timeUnit is DAYS, all items saved before 7 days ago will be removed.
   */
  @CheckResult
  public Single<Integer> recycleAllCachedBefore(int durationBeforeNow, TimeUnit durationTimeUnit) {
    return Single.fromCallable(() -> {
      long daysBeforeNow = durationTimeUnit.toDays(durationBeforeNow);

      LocalDateTime dateTimeBeforeNow = LocalDateTime.now(ZoneId.of("UTC")).minusDays(daysBeforeNow);
      String millisBeforeNowString = String.valueOf(dateTimeBeforeNow.atZone(ZoneId.of("UTC")).toEpochSecond());

      int totalDeletedRows = 0;

      try (BriteDatabase.Transaction transaction = database.newTransaction()) {
        totalDeletedRows += database.delete(CachedSubmissionId.TABLE_NAME, CachedSubmissionId.WHERE_SAVE_TIME_BEFORE, millisBeforeNowString);
        totalDeletedRows += database.delete(
            CachedSubmissionWithoutComments.TABLE_NAME,
            CachedSubmissionWithoutComments.WHERE_SAVE_TIME_BEFORE,
            millisBeforeNowString
        );
        totalDeletedRows += database.delete(
            CachedSubmissionWithComments.TABLE_NAME,
            CachedSubmissionWithComments.WHERE_UPDATE_TIME_BEFORE,
            millisBeforeNowString
        );
        transaction.markSuccessful();
      }

      return totalDeletedRows;
    });
  }

  @AutoValue
  abstract static class SaveResult {
    public abstract List<Submission> savedItems();

    public static SaveResult create(List<Submission> savedItems) {
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

// ======== SAVE ======== //

  public void markAsSaved(PostedOrInFlightContribution submission) {
    savedSubmissionIds.add(submission.fullName());
  }

  public void markAsUnsaved(PostedOrInFlightContribution submission) {
    savedSubmissionIds.remove(submission.fullName());
  }

  public boolean isSaved(PostedOrInFlightContribution submission) {
    return savedSubmissionIds.contains(submission.fullName());
  }
}
