package me.saket.dank.ui.subreddits;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Submission;

import java.util.List;

import me.saket.dank.ui.submission.CachedSubmissionFolder;

@AutoValue
public abstract class SubmissionListUiModel {

  public abstract List<Submission> submissions();

  /**
   * Note: Initial load when DB is empty for a {@link CachedSubmissionFolder} is also treated as pagination.
   */
  public abstract NetworkCallStatus paginationStatus();

  /**
   * Refreshing submissions to override existing submissions.
   */
  public abstract NetworkCallStatus refreshStatus();

  public static SubmissionListUiModel create(List<Submission> submissions, NetworkCallStatus paginationStatus, NetworkCallStatus refreshStatus) {
    return new AutoValue_SubmissionListUiModel(submissions, paginationStatus, refreshStatus);
  }
}
