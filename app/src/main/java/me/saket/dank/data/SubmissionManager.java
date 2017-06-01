package me.saket.dank.data;

import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV2Observable;
import static hu.akarnokd.rxjava.interop.RxJavaInterop.toV2Single;
import static me.saket.dank.utils.CommonUtils.toImmutable;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.CheckResult;

import com.squareup.moshi.Moshi;
import com.squareup.sqlbrite.BriteDatabase;

import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.submission.CachedSubmission;

/**
 * Handles saving and un-saving submissions.
 */
public class SubmissionManager {

  private DankRedditClient dankRedditClient;
  private BriteDatabase briteDatabase;
  private Moshi moshi;
  private Set<String> savedSubmissionIds;

  public SubmissionManager(DankRedditClient dankRedditClient, BriteDatabase briteDatabase, Moshi moshi) {
    this.dankRedditClient = dankRedditClient;
    this.briteDatabase = briteDatabase;
    this.moshi = moshi;
    this.savedSubmissionIds = new HashSet<>();
  }

// ======== GET ======== //

  @CheckResult
  public Observable<List<CachedSubmission>> submissions(String subredditName, SortingAndTimePeriod sortingInfo) {
    return toV2Observable(briteDatabase
        .createQuery(CachedSubmission.TABLE_NAME, CachedSubmission.constructQueryToGetAll(subredditName, sortingInfo))
        .mapToList(CachedSubmission.mapFromCursor(moshi)))
        .map(toImmutable());
  }

  @CheckResult
  public Single<List<CachedSubmission>> fetchMoreSubmissions(String subredditName, SortingAndTimePeriod sortingInfo) {
    return paginationAnchor(subredditName)
        .flatMap(anchor -> fetchSubmissionsFromAnchor(subredditName, sortingInfo, anchor))
        .map(saveMessages(subredditName, sortingInfo, false /* removeExistingSubmissions */))
        .map(toImmutable());
  }

  /**
   * Fetch fresh submissions and remove any existing submissions under <var>subreddit</var> and <var>sortingInfo</var>.
   */
  @CheckResult
  public Single<List<CachedSubmission>> refreshSubmissions(String subredditName, SortingAndTimePeriod sortingInfo) {
    return fetchSubmissionsFromAnchor(subredditName, sortingInfo, PaginationAnchor.createEmpty())
        .map(saveMessages(subredditName, sortingInfo, true /* removeExistingSubmissions */))
        .map(toImmutable());
  }

  /**
   * @param removeExistingSubmissions Whether to remove existing submissions under <var>subredditName</var>.
   */
  private Function<List<Submission>, List<CachedSubmission>> saveMessages(String subredditName, SortingAndTimePeriod sortingInfo,
      boolean removeExistingSubmissions)
  {
    return fetchedSubmissions -> {
      List<CachedSubmission> cachedSubmissions = new ArrayList<>(fetchedSubmissions.size());

      try (BriteDatabase.Transaction transaction = briteDatabase.newTransaction()) {
        if (removeExistingSubmissions) {
          briteDatabase.delete(CachedSubmission.TABLE_NAME, CachedSubmission.WHERE_SUBREDDIT_NAME);
        }

        for (int i = 0; i < fetchedSubmissions.size(); i++) {
          // Reddit sends submissions according to their sorting order. So they may or may not be
          // sorted by their creation time. However, we want to store their download time so that
          // they can be fetched in the same order (because SQLite doesn't guarantee preservation
          // of insertion order). Adding the index to avoid duplicate times.
          long fetchedTimeMillis = System.currentTimeMillis() + i;
          Submission submission = fetchedSubmissions.get(i);
          boolean isSavedByUser = false;  // TODO: Get this value from DB.

          CachedSubmission cachedSubmission = CachedSubmission.create(
              submission.getFullName(),
              submission,
              subredditName,
              sortingInfo,
              fetchedTimeMillis,
              submission.getVote(),
              isSavedByUser
          );
          cachedSubmissions.add(cachedSubmission);
          briteDatabase.insert(CachedSubmission.TABLE_NAME, cachedSubmission.toContentValues(moshi), SQLiteDatabase.CONFLICT_REPLACE);
        }

        transaction.markSuccessful();
      }

      return cachedSubmissions;
    };
  }

  private Single<List<Submission>> fetchSubmissionsFromAnchor(String subredditName, SortingAndTimePeriod sortingInfo, PaginationAnchor anchor) {
    return dankRedditClient.withAuth(Single.fromCallable(() -> {
      SubredditPaginator subredditPaginator = Dank.reddit().subredditPaginator(subredditName);
      if (!anchor.isEmpty()) {
        subredditPaginator.setStartAfterThing(anchor.fullName());
      }
      subredditPaginator.setSorting(sortingInfo.sortOrder());
      subredditPaginator.setTimePeriod(sortingInfo.timePeriod());
      return subredditPaginator.next(true);
    }));
  }

  /**
   * Create a PaginationAnchor from the last cached submission under <var>subredditName</var>.
   */
  @CheckResult
  private Single<PaginationAnchor> paginationAnchor(String subredditName) {
    return toV2Single(briteDatabase
        .createQuery(CachedSubmission.TABLE_NAME, CachedSubmission.QUERY_GET_LAST_IN_FOLDER, subredditName)
        .mapToList(CachedSubmission.mapFromCursor(moshi))
        // cachedSubmissions will only have one value because CachedSubmission.QUERY_GET_LAST_IN_FOLDER places a limit of 1.
        .map(cachedSubmissions -> cachedSubmissions.isEmpty()
            ? PaginationAnchor.createEmpty()
            : PaginationAnchor.create(cachedSubmissions.get(0).fullName()))
        .toSingle());
  }

// ======== SAVE ======== //

  public void save(Submission submission) {
    savedSubmissionIds.add(submission.getId());
  }

  public void unSave(Submission submission) {
    savedSubmissionIds.remove(submission.getId());
  }

  public boolean isSaved(Submission submission) {
    return savedSubmissionIds.contains(submission.getId());
  }
}
