package me.saket.dank.ui.subreddit;

import com.google.auto.value.AutoValue;

import me.saket.dank.ui.submission.CachedSubmissionFolder;

@AutoValue
public abstract class SubmissionsDatabaseAndNetworkState {

  /**
   * Whether the DB has cached submissions for a {@link CachedSubmissionFolder}.
   */
  public abstract boolean hasItemsInDatabase();

  public abstract NetworkCallStatus networkCallStatus();

  public static SubmissionsDatabaseAndNetworkState create(boolean hasItemsInDatabase, NetworkCallStatus networkCallStatus) {
    return new AutoValue_SubmissionsDatabaseAndNetworkState(hasItemsInDatabase, networkCallStatus);
  }
}
