package me.saket.dank.ui.submission;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.CheckResult;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.sqlbrite2.BriteDatabase;

import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.PaginationAnchor;
import me.saket.dank.data.VotingManager;
import me.saket.dank.utils.Commons;
import me.saket.dank.utils.DankSubmissionRequest;
import timber.log.Timber;

@Singleton
public class SubmissionRepository {

  private final BriteDatabase database;
  private final Moshi moshi;
  private final DankRedditClient dankRedditClient;
  private final VotingManager votingManager;

  @Inject
  public SubmissionRepository(BriteDatabase briteDatabase, Moshi moshi, DankRedditClient dankRedditClient, VotingManager votingManager) {
    this.database = briteDatabase;
    this.moshi = moshi;
    this.dankRedditClient = dankRedditClient;
    this.votingManager = votingManager;
  }

  /**
   * Get from DB or from the network if not present in DB.
   */
  @CheckResult
  public Observable<Submission> submissionWithComments(DankSubmissionRequest submissionRequest) {
    String requestJson = moshi.adapter(DankSubmissionRequest.class).toJson(submissionRequest);

    return database.createQuery(CachedSubmissionWithComments.TABLE_NAME, CachedSubmissionWithComments.SELECT_BY_REQUEST_JSON, requestJson)
        .mapToList(CachedSubmissionWithComments.cursorMapper(moshi))
        .flatMap(cachedSubmissions -> {
          if (cachedSubmissions.isEmpty()) {
            // Starting a new Observable here so that it doesn't get canceled when the DB stream is disposed.
            fetchCommentsFromRemote(submissionRequest)
                .subscribeOn(Schedulers.io())
                .subscribe();
            return Observable.empty();

          } else {
            return Observable.just(cachedSubmissions.get(0).submission());
          }
        });
  }

  @CheckResult
  private Completable fetchCommentsFromRemote(DankSubmissionRequest submissionRequest) {
    return dankRedditClient.submission(submissionRequest)
        .flatMapCompletable(submission -> Completable.fromAction(() -> {
          long saveTimeMillis = System.currentTimeMillis();
          CachedSubmissionWithComments cachedSubmission = CachedSubmissionWithComments.create(submissionRequest, submission, saveTimeMillis);
          database.insert(CachedSubmissionWithComments.TABLE_NAME, cachedSubmission.toContentValues(moshi));
        }));
  }

  @CheckResult
  public Observable<List<Submission>> submissions(CachedSubmissionFolder folder) {
    // Ideally we should pass both the tables joined by CachedSubmissionList: CachedSubmissionIds and
    // CachedSubmissionWithoutComments, but since they both get updated at the same time, we don't need
    String sqlQuery = CachedSubmissionList.constructQueryToGetAll(moshi, folder.subredditName(), folder.sortingAndTimePeriod());
    return database.createQuery(CachedSubmissionId.TABLE_NAME, sqlQuery)
        .mapToList(CachedSubmissionList.cursorMapper(moshi.adapter(Submission.class)))
        .map(Commons.toImmutable());
  }

  @CheckResult
  public Single<FetchResult> loadMoreSubmissions(CachedSubmissionFolder folder) {
    return lastPaginationAnchor(folder)
        .doOnSuccess(anchor -> Timber.i("anchor: %s", anchor))
        .map(anchor -> {
          List<Submission> distinctNewItems = new ArrayList<>();
          PaginationAnchor nextAnchor = anchor;

          while (distinctNewItems.size() < 10) {
            FetchResult fetchResult = fetchSubmissionsRemoteWithAnchor(folder, nextAnchor).blockingGet();
            Timber.i("Found %s submissions on remote", fetchResult.fetchedSubmissions().size());

            SaveResult saveResult = saveSubmissions(fetchResult.fetchedSubmissions(), folder).blockingGet();
            distinctNewItems.addAll(saveResult.savedItems());

            if (!fetchResult.hasMoreItems()) {
              break;
            }
            Timber.i("not enough");

            Submission lastFetchedSubmission = fetchResult.fetchedSubmissions().get(fetchResult.fetchedSubmissions().size() - 1);
            nextAnchor = PaginationAnchor.create(lastFetchedSubmission.getFullName());
          }

          return FetchResult.create(Collections.unmodifiableList(distinctNewItems), distinctNewItems.size() > 0);
        });
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
  private Single<FetchResult> fetchSubmissionsRemoteWithAnchor(CachedSubmissionFolder folder, PaginationAnchor anchor) {
    Single<FetchResult> fetchResultSingle = dankRedditClient.withAuth(Single.fromCallable(() -> {
      SubredditPaginator subredditPaginator = dankRedditClient.subredditPaginator(folder.subredditName());
      if (!anchor.isEmpty()) {
        subredditPaginator.setStartAfterThing(anchor.fullName());
      }

      subredditPaginator.setSorting(folder.sortingAndTimePeriod().sortOrder());
      subredditPaginator.setTimePeriod(folder.sortingAndTimePeriod().timePeriod());
      Listing<Submission> submissions = subredditPaginator.next(true);

      return FetchResult.create(submissions, subredditPaginator.hasNext());
    }));

    return fetchResultSingle
        .flatMap(fetchResult -> votingManager
            .removePendingVotesForFetchedSubmissions(fetchResult.fetchedSubmissions())
            .andThen(Single.just(fetchResult))
        );
  }

  /**
   * Note: This will ignore duplicates.
   *
   * @return Saved submissions with duplicates ignored.
   */
  @CheckResult
  private Single<SaveResult> saveSubmissions(List<Submission> submissionsToSave, CachedSubmissionFolder folder) {
    return Single.fromCallable(() -> {
      List<Submission> cachedSubmissions = new ArrayList<>(submissionsToSave.size());
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
            cachedSubmissions.add(submission);
          }
        }

        transaction.markSuccessful();
      }

      return SaveResult.create(Collections.unmodifiableList(cachedSubmissions));
    });
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

        Timber.i("removed all");
      }
    });
  }

  public CompletableSource clearCachedSubmissions() {
    return Completable.fromAction(() -> database.delete(CachedSubmissionWithComments.TABLE_NAME, null));
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
}
