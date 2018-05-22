package me.saket.dank.ui.submission;

import static io.reactivex.schedulers.Schedulers.io;
import static junit.framework.Assert.assertEquals;
import static me.saket.dank.utils.Arrays2.immutable;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.CheckResult;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxbinding2.internal.Notification;
import com.nytimes.android.external.cache3.Cache;
import com.nytimes.android.external.cache3.CacheBuilder;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.sqlbrite2.BriteDatabase;

import net.dean.jraw.http.NetworkException;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import me.saket.dank.BuildConfig;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.PaginationAnchor;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.reply.ReplyRepository;
import me.saket.dank.ui.submission.AuditedCommentSort.SelectedBy;
import me.saket.dank.ui.subreddit.SubmissionPaginationResult;
import me.saket.dank.ui.subreddit.SubredditSearchResult;
import me.saket.dank.ui.subscriptions.SubscriptionRepository;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.Pair;
import me.saket.dank.vote.VotingManager;
import me.saket.dank.walkthrough.SyntheticData;
import timber.log.Timber;

@Singleton
public class SubmissionRepository {

  private final Lazy<Moshi> moshi;
  private final Lazy<BriteDatabase> database;
  private final Lazy<DankRedditClient> dankRedditClient;
  private final Lazy<VotingManager> votingManager;
  private final Lazy<ErrorResolver> errorResolver;
  private final Lazy<SubscriptionRepository> subscriptionRepository;
  private final Lazy<SyntheticData> syntheticData;
  private final Lazy<ReplyRepository> replyRepository;

  private Cache<DankSubmissionRequest, CachedSubmissionWithComments> inMemoryCache;

  @Inject
  public SubmissionRepository(
      Lazy<Moshi> moshi,
      Lazy<BriteDatabase> briteDatabase,
      Lazy<DankRedditClient> dankRedditClient,
      Lazy<VotingManager> votingManager,
      Lazy<ErrorResolver> errorResolver,
      Lazy<SubscriptionRepository> subscriptionRepository,
      Lazy<ReplyRepository> replyRepository,
      Lazy<SyntheticData> syntheticData)
  {
    this.database = briteDatabase;
    this.moshi = moshi;
    this.dankRedditClient = dankRedditClient;
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
  public Observable<Pair<DankSubmissionRequest, Submission>> submissionWithComments(DankSubmissionRequest oldRequest) {
    if (oldRequest.id().equalsIgnoreCase(syntheticData.get().SUBMISSION_ID_FOR_GESTURE_WALKTHROUGH)) {
      return syntheticSubmissionForGesturesWalkthrough()
          .map(syntheticSubmission -> {
            DankSubmissionRequest updatedRequest = oldRequest.toBuilder()
                .commentSort(syntheticSubmission.getSuggestedSort(), SelectedBy.DEFAULT)
                .build();
            return Pair.create(updatedRequest, syntheticSubmission);
          })
          .toObservable();
    }

    Observable<Pair<DankSubmissionRequest, CachedSubmissionWithComments>> dbStream = getFromDbOrFetchSubmissionWithComments(oldRequest)
        .take(1)
        .flatMap(submissionWithComments -> {
          // The aim is to always load comments in the sort mode suggested by a subreddit. In case we
          // load with the wrong sort (possibly because the submission's details were unknown), reload
          // comments using the suggested sort.
          CommentSort suggestedSort = submissionWithComments.submission().getSuggestedSort();
          Boolean useSuggestedSort = suggestedSort != null && oldRequest.commentSort().canOverrideWithSuggestedSort();

          if (useSuggestedSort) {
            //Timber.i("Different sort.");
            DankSubmissionRequest newRequest = oldRequest.toBuilder()
                .commentSort(suggestedSort, SelectedBy.SUBMISSION_SUGGESTED)
                .build();

            return getFromDbOrFetchSubmissionWithComments(newRequest)
                .map(submissions -> Pair.create(newRequest, submissions));

          } else {
            // We're calling getOrFetch() again to receive a refreshing Observable.
            // This should return immediately because the store has an in-memory cache.
            return getFromDbOrFetchSubmissionWithComments(oldRequest)
                .skip(1)
                .startWith(submissionWithComments)
                .map(submissions -> Pair.create(oldRequest, submissions))
                //.compose(RxUtils.doOnceOnNext(o -> Timber.i("Returned from memory")))
                ;
          }
        });

    Observable<Pair<DankSubmissionRequest, CachedSubmissionWithComments>> cachedSubmissions = dbStream
        .replay()
        .refCount();

    Observable<Pair<DankSubmissionRequest, Submission>> cachedSubmissionsWithMemoryCacheUpdates = cachedSubmissions
        .doOnNext(pair -> inMemoryCache.put(pair.first(), pair.second()))
        .startWith(Observable.create(emitter -> {
          CachedSubmissionWithComments inMemoryValue = inMemoryCache.getIfPresent(oldRequest);
          if (inMemoryValue != null) {
            emitter.onNext(Pair.create(oldRequest, inMemoryValue));
          }
          emitter.onComplete();
        }))
        .distinctUntilChanged((first, second) -> first.second().updateTimeMillis() == second.second().updateTimeMillis())
        .map(pair -> Pair.create(pair.first(), pair.second().submission()));

    Observable<Pair<DankSubmissionRequest, Submission>> staleUpdates = cachedSubmissions
        .take(1)
        .observeOn(io())
        .flatMapCompletable(pair -> {
          // Dank unfortunately has two storage locations for submissions:
          // - one for submission list
          // - one for submission with comments.
          //
          // I need to figure out to use a single storage for both, but for now keeping both of
          // them in sync is the only option. When reading submissions with comments, we'll check
          // if an updated copy is available in the submission list table. We do the reverse when
          // saving a submission with comments. See saveSubmissionWithAndWithoutComments().
          DankSubmissionRequest updatedSubmissionRequest = pair.first();
          CachedSubmissionWithComments possiblyStaleSubWithComments = pair.second();

          // There was a bug earlier, where cached-submission-WITH-comments and
          // cached-submission-WITHOUT-comments were getting saved a few millis apart
          // so the DB used to always return a submission as stale even if it was just saved.
          // Although it was fixed, I'm adding a small gap here just to make sure if doesn't
          // happen again.
          long targetSaveTimeMillis = possiblyStaleSubWithComments.updateTimeMillis();

          return database.get()
              .createQuery(
                  CachedSubmissionWithoutComments.TABLE_NAME,
                  CachedSubmissionWithoutComments.SELECT_BY_FULLNAME_AND_SAVE_TIME_NEWER_THAN,
                  possiblyStaleSubWithComments.submission().getFullName(),
                  String.valueOf(targetSaveTimeMillis)
              )
              .mapToList(cursor -> CachedSubmissionWithoutComments.createFromCursor(cursor, moshi.get()))
              .take(1)
              .flatMap(items -> items.isEmpty() ? Observable.empty() : Observable.just(items.get(0)))
              .doOnNext(o -> Timber.i("Found one submission: %s", o.saveTimeMillis()))
              .map(optionalNewerSubWithoutComments -> CachedSubmissionWithComments.create(
                  updatedSubmissionRequest,
                  new Submission(
                      optionalNewerSubWithoutComments.submission().getDataNode(),
                      possiblyStaleSubWithComments.submission().getComments()
                  ),
                  optionalNewerSubWithoutComments.saveTimeMillis()
              ))
              .flatMapCompletable(newCachedSub -> Completable.fromAction(() ->
                  database.get().insert(
                      CachedSubmissionWithComments.TABLE_NAME,
                      newCachedSub.toContentValues(moshi.get()),
                      SQLiteDatabase.CONFLICT_REPLACE)
              ));
        })
        .andThen(Observable.empty());

    return cachedSubmissionsWithMemoryCacheUpdates
        .mergeWith(staleUpdates);
  }

  /**
   * Get from DB or from the network if not present in DB.
   */
  @CheckResult
  private Observable<CachedSubmissionWithComments> getFromDbOrFetchSubmissionWithComments(DankSubmissionRequest request) {
    //Timber.i("Reading for %s", request);
    String requestJson = moshi.get().adapter(DankSubmissionRequest.class).toJson(request);

    Observable<List<CachedSubmissionWithComments>> sharedDbStream = database.get()
        .createQuery(CachedSubmissionWithComments.TABLE_NAME, CachedSubmissionWithComments.SELECT_BY_REQUEST_JSON, requestJson)
        .mapToList(CachedSubmissionWithComments.cursorMapper(moshi.get()))
        .share();

    Observable<CachedSubmissionWithComments> fetchObservable = sharedDbStream
        .map(dbItems -> dbItems.isEmpty())
        .flatMapCompletable(isDbEmpty -> {
          if (isDbEmpty) {
            Single<Submission> sharedSubmissionStream = dankRedditClient.get().submission(request);

            Completable saveCompletable = sharedSubmissionStream
                .map(submission -> CachedSubmissionWithComments.create(request, submission, System.currentTimeMillis()))
                .flatMapCompletable(this::saveSubmissionWithAndWithoutComments);

            return saveCompletable
                .mergeWith(sharedSubmissionStream
                    .map(ParentThread::of)
                    .flatMapCompletable(replyRepository.get()::removeSyncPendingPostedReplies));

          } else {
            return Completable.complete();
          }
        })
        .andThen(Observable.empty());

    return sharedDbStream
        .flatMap(dbItems -> dbItems.isEmpty() ? Observable.empty() : Observable.just(dbItems.get(0)))
        .mergeWith(fetchObservable);
  }

  @CheckResult
  private Completable saveSubmissionWithAndWithoutComments(CachedSubmissionWithComments cachedSubmission) {
    return Completable.fromAction(() -> {
      //Timber.i("Saving submission with comments: %s", submission.getTitle());

      Submission submissionWithoutComments = new Submission(cachedSubmission.submission().getDataNode());
      CachedSubmissionWithoutComments cachedSubmissionWithoutComments = CachedSubmissionWithoutComments.create(
          cachedSubmission.submission().getFullName(),
          submissionWithoutComments,
          submissionWithoutComments.getSubredditName(),
          cachedSubmission.updateTimeMillis()
      );
      JsonAdapter<Submission> submissionJsonAdapter = moshi.get().adapter(Submission.class);
      ContentValues valuesWithoutComments = cachedSubmissionWithoutComments.toContentValues(submissionJsonAdapter);
      ContentValues valuesWithComments = cachedSubmission.toContentValues(moshi.get());

      try (BriteDatabase.Transaction transaction = database.get().newTransaction()) {
        database.get().insert(CachedSubmissionWithComments.TABLE_NAME, valuesWithComments, SQLiteDatabase.CONFLICT_REPLACE);
        database.get().insert(CachedSubmissionWithoutComments.TABLE_NAME, valuesWithoutComments, SQLiteDatabase.CONFLICT_REPLACE);
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
    return dankRedditClient.get().withAuth(dankRedditClient.get().loadMoreComments(commentNode))
        .toCompletable()
        .andThen(saveSubmissionWithAndWithoutComments(CachedSubmissionWithComments.create(
            submissionRequest,
            submission,
            System.currentTimeMillis()
        )));
  }

  public Completable clearCachedSubmissionWithComments(DankSubmissionRequest request) {
    return Completable.fromAction(() -> {
      inMemoryCache.invalidate(request);
      String requestJson = moshi.get().adapter(DankSubmissionRequest.class).toJson(request);
      database.get().delete(CachedSubmissionWithComments.TABLE_NAME, CachedSubmissionWithComments.WHERE_REQUEST_JSON, requestJson);
    });
  }

  @CheckResult
  public Completable clearAllCachedSubmissionWithComments() {
    if (!BuildConfig.DEBUG) {
      throw new AssertionError();
    }
    return Completable.fromAction(() -> {
      inMemoryCache.invalidateAll();

      try (BriteDatabase.Transaction transaction = database.get().newTransaction()) {
        database.get().delete(CachedSubmissionWithComments.TABLE_NAME, null);
        transaction.markSuccessful();
      }
    });
  }

  @CheckResult
  public Single<Submission> syntheticSubmissionForGesturesWalkthrough() {
    return Single.fromCallable(() -> {
      JsonAdapter<Submission> adapter = moshi.get().adapter(Submission.class);
      return adapter.fromJson(syntheticData.get().submissionForGesturesWalkthroughJson());
    });
  }

// ======== SUBMISSION LIST (W/O COMMENTS) ======== //

  @CheckResult
  public Observable<List<Submission>> submissions(CachedSubmissionFolder folder) {
    List<String> tablesToListen = Arrays.asList(CachedSubmissionId.TABLE_NAME, CachedSubmissionWithoutComments.TABLE_NAME);
    String sqlQuery = CachedSubmissionList.constructQueryToGetAll(moshi.get(), folder.subredditName(), folder.sortingAndTimePeriod());

    return database.get().createQuery(tablesToListen, sqlQuery)
        .mapToList(CachedSubmissionList.cursorMapper(moshi.get().adapter(Submission.class)))
        .as(immutable());
  }

  /**
   * @return Operates on the main thread.
   */
  @CheckResult
  public Observable<SubmissionPaginationResult> loadAndSaveMoreSubmissions(CachedSubmissionFolder folder) {
    return lastPaginationAnchor(folder)
        //.doOnSuccess(anchor -> Timber.i("anchor: %s", anchor))
        .flatMapCompletable(anchor -> dankRedditClient.get()
            .withAuth(Single.fromCallable(() -> {
              List<Submission> distinctNewItems = new ArrayList<>();
              PaginationAnchor nextAnchor = anchor;
              int savedSubmissionCount = 0;

              while (true) {
                FetchResult fetchResult = fetchSubmissionsFromRemoteWithAnchor(folder, nextAnchor);
                votingManager.get().removePendingVotesForFetchedSubmissions(fetchResult.fetchedSubmissions()).subscribe();
                //Timber.i("Found %s submissions on remote", fetchResult.fetchedSubmissions().size());

                SaveResult saveResult = saveSubmissions(folder, fetchResult.fetchedSubmissions());
                distinctNewItems.addAll(saveResult.savedItems());
                savedSubmissionCount += saveResult.savedItems().size();

                if (!fetchResult.hasMoreItems() || distinctNewItems.size() > 10) {
                  //Timber.i("Breaking early");
                  break;
                }

                //Timber.i("%s distinct items not enough", distinctNewItems.size());
                Submission lastFetchedSubmission = fetchResult.fetchedSubmissions().get(fetchResult.fetchedSubmissions().size() - 1);
                nextAnchor = PaginationAnchor.create(lastFetchedSubmission.getFullName());
              }
              //Timber.i("Fetched %s submissions", distinctNewItems.size());
              return savedSubmissionCount;
            }))
            .flatMapCompletable(savedSubmissionCount -> {
              if (savedSubmissionCount == 0 && anchor.isEmpty()) {
                return dankRedditClient.get()
                    .findSubreddit2(folder.subredditName())
                    .flatMapCompletable(searchResult -> {
                      switch (searchResult.type()) {
                        case SUCCESS:
                          return Completable.<SubmissionPaginationResult>complete();

                        case ERROR_PRIVATE:
                          throw new AssertionError("Submission paginator throws an 403 for private subreddit. Should never reach here");

                        case ERROR_NOT_FOUND:
                          return Completable.error(new SubredditNotFoundException());

                        case ERROR_UNKNOWN:
                          return Completable.error(((SubredditSearchResult.UnknownError) searchResult).error());

                        default:
                          return Completable.error(new AssertionError("Unknown error getting submissions for " + folder));
                      }
                    });
              } else {
                return Completable.<SubmissionPaginationResult>complete();
              }
            })
            .onErrorResumeNext(error -> {
              if (error instanceof NetworkException && ((NetworkException) error).getResponse().getStatusCode() == 403) {
                return Completable.error(new PrivateSubredditException());
              } else {
                return Completable.error(error);
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
    String sortingAndTimePeriodJson = moshi.get().adapter(SortingAndTimePeriod.class).toJson(folder.sortingAndTimePeriod());
    String sqlQuery = CachedSubmissionId.constructQueryToGetLastSubmission(folder.subredditName(), sortingAndTimePeriodJson);

    Function<Cursor, PaginationAnchor> paginationAnchorFunction = cursor -> {
      //noinspection CodeBlock2Expr
      return PaginationAnchor.create(CachedSubmissionId.SUBMISSION_FULLNAME_MAPPER.apply(cursor));
    };

    return database.get()
        .createQuery(CachedSubmissionId.TABLE_NAME, sqlQuery)
        .mapToOneOrDefault(paginationAnchorFunction, PaginationAnchor.createEmpty())
        .take(1)
        .firstOrError();
  }

  @CheckResult
  private FetchResult fetchSubmissionsFromRemoteWithAnchor(CachedSubmissionFolder folder, PaginationAnchor anchor) {
    boolean isFrontpage = subscriptionRepository.get().isFrontpage(folder.subredditName());
    SubredditPaginator subredditPaginator = dankRedditClient.get().subredditPaginator(folder.subredditName(), isFrontpage);
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
    JsonAdapter<SortingAndTimePeriod> andTimePeriodJsonAdapter = moshi.get().adapter(SortingAndTimePeriod.class);
    JsonAdapter<Submission> submissionJsonAdapter = moshi.get().adapter(Submission.class);

    List<ContentValues> submissionIdValuesList = new ArrayList<>(submissionsToSave.size());
    List<ContentValues> submissionWithoutCommentsValuesList = new ArrayList<>(submissionsToSave.size());

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

      submissionIdValuesList.add(cachedSubmissionId.toContentValues(andTimePeriodJsonAdapter));
      submissionWithoutCommentsValuesList.add(cachedSubmissionWithoutComments.toContentValues(submissionJsonAdapter));
    }

    List<Submission> savedSubmissions = new ArrayList<>(submissionsToSave.size());

    Timber.i("save 2");
    try (BriteDatabase.Transaction transaction = database.get().newTransaction()) {
      for (int i = 0; i < submissionIdValuesList.size(); i++) {
        ContentValues submissionIdValues = submissionIdValuesList.get(i);
        ContentValues submissionWithoutCommentsValues = submissionWithoutCommentsValuesList.get(i);

        // Again, since Reddit does not send submissions sorted by time, it's possible to receive
        // duplicate submissions.Ignore them.
        long insertedRowId = database.get().insert(CachedSubmissionId.TABLE_NAME, submissionIdValues, SQLiteDatabase.CONFLICT_IGNORE);
        if (insertedRowId != -1) {
          // CONFLICT_REPLACE: to handle updated submissions received from remote.
          database.get().insert(CachedSubmissionWithoutComments.TABLE_NAME, submissionWithoutCommentsValues, SQLiteDatabase.CONFLICT_REPLACE);
          Submission savedSubmission = submissionsToSave.get(i);
          assertEquals(submissionIdValues.get(CachedSubmissionId.COLUMN_SUBMISSION_FULL_NAME), savedSubmission.getFullName());
          savedSubmissions.add(savedSubmission);
        }
      }

      transaction.markSuccessful();
    }
    //Timber.i("Saved %d items in: %sms", submissionsToSave.size(), (System.currentTimeMillis() - startTime));

    return SaveResult.create(Collections.unmodifiableList(savedSubmissions));
  }

  public Completable clearCachedSubmissionLists() {
    return Completable.fromAction(() -> {
      try (BriteDatabase.Transaction transaction = database.get().newTransaction()) {
        database.get().delete(CachedSubmissionId.TABLE_NAME, null);
        database.get().delete(CachedSubmissionWithoutComments.TABLE_NAME, null);
        transaction.markSuccessful();
      }
    });
  }

  public Completable clearCachedSubmissionLists(String subredditName) {
    return Completable.fromAction(() -> {
      try (BriteDatabase.Transaction transaction = database.get().newTransaction()) {
        database.get().delete(CachedSubmissionId.TABLE_NAME, CachedSubmissionId.WHERE_SUBREDDIT_NAME, subredditName);
        database.get().delete(CachedSubmissionWithoutComments.TABLE_NAME, CachedSubmissionWithoutComments.WHERE_SUBREDDIT_NAME, subredditName);
        transaction.markSuccessful();
      }
    });
  }

  /**
   * Recycle all items that were saved X millis before now. If <var>durationFromNow</var> is 7 and
   * timeUnit is DAYS, all items saved before 7 days ago will be removed.
   */
  @CheckResult
  public Single<Integer> recycleAllCachedBefore(int durationFromNow, TimeUnit durationTimeUnit) {
    long daysBeforeNow = durationTimeUnit.toDays(durationFromNow);
    LocalDateTime dateTimeBeforeNow = LocalDateTime.now(ZoneId.of("UTC")).minusDays(daysBeforeNow);
    String millisBeforeNowString = String.valueOf(dateTimeBeforeNow.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli());

    Completable logCompletable;

    if (BuildConfig.DEBUG) {
      logCompletable = Completable.fromAction(() -> {
        List<CachedSubmissionWithComments> rowsToBeDeleted = database.get()
            .createQuery(CachedSubmissionWithComments.TABLE_NAME, CachedSubmissionWithComments.SELECT_WHERE_UPDATE_TIME_BEFORE, millisBeforeNowString)
            .mapToList(CachedSubmissionWithComments.cursorMapper(moshi.get()))
            .blockingFirst();

        Timber.i("Now time: %s", System.currentTimeMillis());

        Timber.i("Recycling rows before %s (%s): %s", millisBeforeNowString, dateTimeBeforeNow, rowsToBeDeleted.size());
        for (CachedSubmissionWithComments row : rowsToBeDeleted) {
          Timber.i("%s", row.submission().getTitle());
        }
      });
    } else {
      logCompletable = Completable.complete();
    }

    return logCompletable
        .andThen(Single.fromCallable(() -> {
          int totalDeletedRows = 0;

          try (BriteDatabase.Transaction transaction = database.get().newTransaction()) {
            totalDeletedRows += database.get().delete(CachedSubmissionId.TABLE_NAME, CachedSubmissionId.WHERE_SAVE_TIME_BEFORE, millisBeforeNowString);
            totalDeletedRows += database.get().delete(
                CachedSubmissionWithoutComments.TABLE_NAME,
                CachedSubmissionWithoutComments.WHERE_SAVE_TIME_BEFORE,
                millisBeforeNowString
            );
            totalDeletedRows += database.get().delete(
                CachedSubmissionWithComments.TABLE_NAME,
                CachedSubmissionWithComments.WHERE_UPDATE_TIME_BEFORE,
                millisBeforeNowString
            );
            transaction.markSuccessful();
          }

          return totalDeletedRows;
        }));
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
}
