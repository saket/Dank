package me.saket.dank.data;

import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV2Observable;
import static me.saket.dank.utils.Commons.toImmutable;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.CheckResult;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Moshi;
import com.squareup.sqlbrite.BriteDatabase;

import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.submission.CachedSubmission;
import me.saket.dank.ui.submission.CachedSubmissionFolder;
import timber.log.Timber;

/**
 * Handles saving and un-saving submissions.
 */
public class SubmissionManager {

  private DankRedditClient dankRedditClient;
  private BriteDatabase briteDatabase;
  private VotingManager votingManager;
  private Moshi moshi;
  private Set<String> savedSubmissionIds;

  public SubmissionManager(DankRedditClient dankRedditClient, BriteDatabase briteDatabase, VotingManager votingManager, Moshi moshi) {
    this.dankRedditClient = dankRedditClient;
    this.briteDatabase = briteDatabase;
    this.votingManager = votingManager;
    this.moshi = moshi;
    this.savedSubmissionIds = new HashSet<>();
  }

// ======== CACHING ======== //

  @CheckResult
  public Observable<List<Submission>> submissions(CachedSubmissionFolder folder) {
    return toV2Observable(briteDatabase
        .createQuery(CachedSubmission.TABLE_NAME, CachedSubmission.constructQueryToGetAll(folder))
        .mapToList(CachedSubmission.mapSubmissionFromCursor(moshi)))
        .map(toImmutable());
  }

  @CheckResult
  public Single<List<CachedSubmission>> fetchAndSaveMoreSubmissions(CachedSubmissionFolder folder) {
    return lastPaginationAnchor(folder)
        .doOnSuccess(paginationAnchor -> Timber.i("paginationAnchor: %s", paginationAnchor))
        .map(anchor -> {
          List<CachedSubmission> cachedSubmissions = Collections.emptyList();
          FetchResult fetchResult = FetchResult.create(Collections.emptyList(), true);

          // saveNewSubmissions() ignores duplicates. Which means that we might have to do
          // another fetch if all fetched submissions turned out to be duplicates.
          while (cachedSubmissions.isEmpty() && fetchResult.hasMoreItems()) {
            fetchResult = fetchSubmissionsFromAnchor(folder, anchor).blockingGet();
            List<Submission> fetchedSubmissions = fetchResult.fetchedSubmissions();
            cachedSubmissions = saveNewSubmissions(fetchedSubmissions, folder, false).blockingGet();
          }

          return Collections.unmodifiableList(cachedSubmissions);
        });
  }

  /**
   * Fetch fresh submissions under <var>folder</var>.
   */
  @CheckResult
  public Single<List<Submission>> fetchFromRemote(CachedSubmissionFolder folder) {
    return fetchSubmissionsFromAnchor(folder, PaginationAnchor.createEmpty())
        .map(fetchResult -> fetchResult.fetchedSubmissions());
  }

  /**
   * Fetch and save fresh submissions under <var>folder</var>.
   */
  @CheckResult
  public Single<List<CachedSubmission>> fetchAndSaveFromRemote(CachedSubmissionFolder folder, boolean removeExistingSubmissions) {
    return fetchFromRemote(folder)
        .flatMap(fetchedSubmissions -> Dank.submissions().saveNewSubmissions(fetchedSubmissions, folder, removeExistingSubmissions));
  }

  /**
   * This will ignore duplicates.
   *
   * @param removeExistingSubmissions Whether to remove existing submissions under <var>folder</var>.
   */
  @CheckResult
  private Single<List<CachedSubmission>> saveNewSubmissions(List<Submission> submissionsToSave, CachedSubmissionFolder folder,
      boolean removeExistingSubmissions)
  {
    return Single.fromCallable(() -> {
      List<CachedSubmission> cachedSubmissions = new ArrayList<>(submissionsToSave.size());

      try (BriteDatabase.Transaction transaction = briteDatabase.newTransaction()) {
        if (removeExistingSubmissions) {
          briteDatabase.delete(CachedSubmission.TABLE_NAME, CachedSubmission.constructWhere(folder));
        }

        for (int i = 0; i < submissionsToSave.size(); i++) {
          // Reddit sends submissions according to their sorting order. So they may or may not be
          // sorted by their creation time. However, we want to store their download time so that
          // they can be fetched in the same order (because SQLite doesn't guarantee preservation
          // of insertion order). Adding the index to avoid duplicate times.
          long fetchedTimeMillis = System.currentTimeMillis() + i;
          Submission submission = submissionsToSave.get(i);
          boolean isSavedByUser = false;  // TODO: Get this value from DB.

          CachedSubmission cachedSubmission = CachedSubmission.create(
              submission.getFullName(),
              submission,
              folder,
              fetchedTimeMillis,
              submission.getVote(),
              isSavedByUser
          );

          // It's possible to receive a duplicate submission from Reddit if the submissions got
          // reordered because of changes in their votes. We'll ignore them.
          long inserted = briteDatabase.insert(CachedSubmission.TABLE_NAME, cachedSubmission.toContentValues(moshi), SQLiteDatabase.CONFLICT_IGNORE);
          if (inserted != -1) {
            cachedSubmissions.add(cachedSubmission);
          }
        }

        transaction.markSuccessful();
      }

      return cachedSubmissions;
    });
  }

  @CheckResult
  private Single<FetchResult> fetchSubmissionsFromAnchor(CachedSubmissionFolder folder, PaginationAnchor anchor) {
    Single<FetchResult> fetchResultSingle = dankRedditClient.withAuth(Single.fromCallable(() -> {
      SubredditPaginator subredditPaginator = Dank.reddit().subredditPaginator(folder.subredditName());
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

  @AutoValue
  public abstract static class FetchResult {
    public abstract List<Submission> fetchedSubmissions();

    public abstract boolean hasMoreItems();

    public static FetchResult create(List<Submission> fetchedSubmissions, boolean hasMoreItems) {
      return new AutoValue_SubmissionManager_FetchResult(fetchedSubmissions, hasMoreItems);
    }
  }

  /**
   * Create a PaginationAnchor from the last cached submission under <var>folder</var>.
   */
  @CheckResult
  private Single<PaginationAnchor> lastPaginationAnchor(CachedSubmissionFolder folder) {
    return toV2Observable(briteDatabase
        .createQuery(CachedSubmission.TABLE_NAME, CachedSubmission.constructQueryToGetLastSubmission(folder))
        .mapToList(CachedSubmission.mapFromCursor(moshi)))
        // list will only have one value because the query places a limit of 1.
        .map(cachedSubmissions -> cachedSubmissions.isEmpty()
            ? PaginationAnchor.createEmpty()
            : PaginationAnchor.create(cachedSubmissions.get(0).fullName()))
        .doOnNext(anchor -> Timber.i("anchor: %s", anchor))
        .firstOrError();
  }

  @CheckResult
  public Single<Boolean> hasSubmissions(CachedSubmissionFolder folder) {
    return toV2Observable(briteDatabase.createQuery(CachedSubmission.TABLE_NAME, CachedSubmission.constructQueryToGetCount(folder))
        .mapToOne(cursor -> cursor.getInt(0) > 0))
        .firstOrError();
  }

  @CheckResult
  public Completable removeAllCached() {
    return Completable.fromAction(() -> briteDatabase.delete(CachedSubmission.TABLE_NAME, null));
  }

  @CheckResult
  public Completable removeAllCachedInFolder(CachedSubmissionFolder folder) {
    return Completable.fromAction(() -> briteDatabase.delete(CachedSubmission.TABLE_NAME, CachedSubmission.constructWhere(folder)));
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
