package me.saket.dank.ui.submission;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.CheckResult;
import android.support.v4.util.Pair;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxbinding2.internal.Notification;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;
import com.nytimes.android.external.store3.base.Persister;
import com.nytimes.android.external.store3.base.impl.Store;
import com.nytimes.android.external.store3.base.impl.StoreBuilder;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.sqlbrite2.BriteDatabase;

import net.dean.jraw.models.CommentSort;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.PaginationAnchor;
import me.saket.dank.data.SubredditSubscriptionManager;
import me.saket.dank.data.VotingManager;
import me.saket.dank.ui.subreddits.NetworkCallStatus;
import me.saket.dank.utils.Commons;
import me.saket.dank.utils.DankSubmissionRequest;
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
  private Store<Submission, DankSubmissionRequest> submissionWithCommentsStore;

  @Inject
  public SubmissionRepository(BriteDatabase briteDatabase, Moshi moshi, DankRedditClient dankRedditClient, VotingManager votingManager,
      ErrorResolver errorResolver, SubredditSubscriptionManager subscriptionManager)
  {
    this.database = briteDatabase;
    this.moshi = moshi;
    this.dankRedditClient = dankRedditClient;
    this.votingManager = votingManager;
    this.errorResolver = errorResolver;
    this.subscriptionManager = subscriptionManager;
  }

// ======== SUBMISSION WITH COMMENTS ======== //

  /**
   * Get from DB or from the network if not present in DB.
   *
   * @return Pair of an optionally-updated submission request in case remote suggested a
   * different sort for comments and the submission object with comments.
   */
  @CheckResult
  public Observable<Pair<DankSubmissionRequest, Submission>> submissionWithComments(DankSubmissionRequest submissionRequest) {
    return getOrFetchSubmissionWithComments(submissionRequest)
        .take(1)
        .flatMap(submissionWithComments -> {
          // The aim is to always load comments in the sort mode suggested by a subreddit. In case we
          // load with the wrong sort (possibly because the submission's details were unknown), reload
          // comments using the suggested sort.
          CommentSort suggestedSort = submissionWithComments.getSuggestedSort();
          if (suggestedSort != null && suggestedSort != submissionRequest.commentSort()) {
            DankSubmissionRequest newRequest = submissionRequest.toBuilder()
                .commentSort(suggestedSort)
                .build();
            return getOrFetchSubmissionWithComments(newRequest)
                .map(submissions -> Pair.create(newRequest, submissions));

          } else {
            return getOrFetchSubmissionWithComments(submissionRequest)
                .map(submissions -> Pair.create(submissionRequest, submissions));
          }
        });
  }

  /**
   * Note that this method will not ensure that <var>DankSubmissionRequest</var>'s sort matches
   * with its submission's suggested sort.
   */
//  public Observable<Boolean> hasSubmissionWithCommentsInCache(DankSubmissionRequest submissionRequest) {
//    database.createQuery(CachedSubmissionWithComments.TABLE_NAME, CachedSubmissionWithComments.)
//  }

  /**
   * Get from DB or from the network if not present in DB.
   */
  @CheckResult
  private Observable<Submission> getOrFetchSubmissionWithComments(DankSubmissionRequest submissionRequest) {
    if (submissionWithCommentsStore == null) {
      submissionWithCommentsStore = StoreBuilder.<DankSubmissionRequest, Submission>key()
          .fetcher(request -> dankRedditClient.submission(request))
          .persister(new Persister<Submission, DankSubmissionRequest>() {
            @Nonnull
            @Override
            public Maybe<Submission> read(@Nonnull DankSubmissionRequest submissionRequest) {
              String requestJson = moshi.adapter(DankSubmissionRequest.class).toJson(submissionRequest);

              return database.createQuery(CachedSubmissionWithComments.TABLE_NAME, CachedSubmissionWithComments.SELECT_BY_REQUEST_JSON, requestJson)
                  .mapToList(CachedSubmissionWithComments.cursorMapper(moshi))
                  .firstElement()
                  .flatMap(cachedSubmissions -> {
                    if (cachedSubmissions.isEmpty()) {
                      return Maybe.empty();
                    } else {
                      return Maybe.just(cachedSubmissions.get(0).submission());
                    }
                  });
            }

            @Nonnull
            @Override
            public Single<Boolean> write(@Nonnull DankSubmissionRequest submissionRequest, @Nonnull Submission submission) {
              return Single.fromCallable(() -> {
                Timber.i("Saving submission with comments: %s", submission.getTitle());

                long saveTimeMillis = System.currentTimeMillis();
                CachedSubmissionWithComments cachedSubmission = CachedSubmissionWithComments.create(submissionRequest, submission, saveTimeMillis);

                Submission submissionWithoutComments = new Submission(submission.getDataNode());
                CachedSubmissionWithoutComments cachedSubmissionWithoutComments = CachedSubmissionWithoutComments.create(
                    submission.getFullName(),
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

                return true;
              });
            }
          })
          .open();
    }

    return submissionWithCommentsStore.getRefreshing(submissionRequest);
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

  @CheckResult
  public Observable<Integer> submissionCount(CachedSubmissionFolder folder) {
    String sortingAndTimeJson = moshi.adapter(SortingAndTimePeriod.class).toJson(folder.sortingAndTimePeriod());
    String countQuery = CachedSubmissionId.queryToGetCount(folder.subredditName(), sortingAndTimeJson);
    return database.createQuery(CachedSubmissionId.TABLE_NAME, countQuery)
        .mapToOne(cursor -> cursor.getInt(0));
  }

  /**
   * @return Operates on the main thread.
   */
  @CheckResult
  public Observable<NetworkCallStatus> loadAndSaveMoreSubmissions(CachedSubmissionFolder folder) {
    Relay<NetworkCallStatus> networkCallStatusStream = PublishRelay.create();

    lastPaginationAnchor(folder)
        .doOnSubscribe(o -> networkCallStatusStream.accept(NetworkCallStatus.createInFlight()))
        .doOnSuccess(anchor -> Timber.i("anchor: %s", anchor))
        .flatMap(anchor -> dankRedditClient.withAuth(Single.create(emitter -> {
          List<Submission> distinctNewItems = new ArrayList<>();
          PaginationAnchor nextAnchor = anchor;

          while (true) {
            FetchResult fetchResult = fetchSubmissionsFromRemoteWithAnchor(folder, nextAnchor);
            votingManager.removePendingVotesForFetchedSubmissions(fetchResult.fetchedSubmissions()).subscribe();
            //Timber.i("Found %s submissions on remote", fetchResult.fetchedSubmissions().size());

            SaveResult saveResult = saveSubmissions(folder, fetchResult.fetchedSubmissions());
            distinctNewItems.addAll(saveResult.savedItems());

            if (!fetchResult.hasMoreItems() || distinctNewItems.size() > 10) {
              break;
            }

            //Timber.i("%s distinct items not enough", distinctNewItems.size());
            Submission lastFetchedSubmission = fetchResult.fetchedSubmissions().get(fetchResult.fetchedSubmissions().size() - 1);
            nextAnchor = PaginationAnchor.create(lastFetchedSubmission.getFullName());
          }

          emitter.onSuccess(FetchResult.create(Collections.unmodifiableList(distinctNewItems), distinctNewItems.size() > 0));
        })))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .retryWhen(errors -> errors.flatMap(error -> {
          Throwable actualError = errorResolver.findActualCause(error);

          if (actualError instanceof InterruptedIOException) {
            Timber.w("Retrying on thread interruption");
            return Flowable.just((Object) Notification.INSTANCE);
          } else {
            return Flowable.error(error);
          }
        }))
        .subscribe(
            o -> networkCallStatusStream.accept(NetworkCallStatus.createIdle()),
            error -> {
              Timber.e(error, "Failed to load submissions");
              networkCallStatusStream.accept(NetworkCallStatus.createFailed(error));
            }
        );

    return networkCallStatusStream;
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

    final long startTime = System.currentTimeMillis();
    try (BriteDatabase.Transaction transaction = database.newTransaction()) {
      for (int i = 0; i < submissionsToSave.size(); i++) {
        // Reddit sends submissions according to their sorting order. So they may or may not be
        // sorted by their creation time. However, we want to store their download time so that
        // they can be fetched in the same order (because SQLite doesn't guarantee preservation
        // of insertion order). Adding the index to avoid duplicate times.
        long saveTimeMillis = System.currentTimeMillis() + i;
        Submission submission = submissionsToSave.get(i);

        CachedSubmissionId cachedSubmissionId = CachedSubmissionId.create(
            submission.getFullName(),
            folder.subredditName(),
            folder.sortingAndTimePeriod(),
            saveTimeMillis
        );

        CachedSubmissionWithoutComments cachedSubmissionWithoutComments = CachedSubmissionWithoutComments.create(
            submission.getFullName(),
            submission,
            folder.subredditName(),
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
    Timber.i("Saved %d items in: %sms", submissionsToSave.size(), (System.currentTimeMillis() - startTime));

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

  @AutoValue
  abstract static class SaveResult {
    public abstract List<Submission> savedItems();

    public static SaveResult create(List<Submission> savedItems) {
      return new AutoValue_SubmissionRepository_SaveResult(savedItems);
    }
  }

  @AutoValue
  public abstract static class FetchResult {

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

  public void markAsSaved(Submission submission) {
    savedSubmissionIds.add(submission.getId());
  }

  public void markAsUnsaved(Submission submission) {
    savedSubmissionIds.remove(submission.getId());
  }

  public boolean isSaved(Submission submission) {
    return savedSubmissionIds.contains(submission.getId());
  }
}
