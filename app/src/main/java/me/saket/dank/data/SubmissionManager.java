package me.saket.dank.data;

import static me.saket.dank.utils.Commons.toImmutable;

import android.support.annotation.CheckResult;

import com.squareup.moshi.Moshi;
import com.squareup.sqlbrite2.BriteDatabase;

import net.dean.jraw.models.Submission;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Observable;
import me.saket.dank.ui.submission.CachedSubmissionDeprecated;
import me.saket.dank.ui.submission.CachedSubmissionFolder;

/**
 * Handles saving and un-saving submissions.
 *
 * @deprecated TODO: use SubmissionRepository instead.
 */
public class SubmissionManager {

  private BriteDatabase briteDatabase;
  private Moshi moshi;
  private Set<String> savedSubmissionIds;

  public SubmissionManager(BriteDatabase briteDatabase, Moshi moshi) {
    this.briteDatabase = briteDatabase;
    this.moshi = moshi;
    this.savedSubmissionIds = new HashSet<>();
  }

// ======== CACHING ======== //

  @CheckResult
  public Observable<List<Submission>> submissions(CachedSubmissionFolder folder) {
    return briteDatabase
        .createQuery(CachedSubmissionDeprecated.TABLE_NAME, CachedSubmissionDeprecated.constructQueryToGetAll(folder))
        .mapToList(CachedSubmissionDeprecated.mapSubmissionFromCursor(moshi))
        .map(toImmutable());
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
