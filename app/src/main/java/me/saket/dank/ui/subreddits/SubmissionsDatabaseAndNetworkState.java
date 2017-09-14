package me.saket.dank.ui.subreddits;

import com.google.auto.value.AutoValue;

import me.saket.dank.ui.submission.CachedSubmissionFolder;

@AutoValue
public abstract class SubmissionsDatabaseAndNetworkState {

  /**
   * Whether the DB has cached submissions for a {@link CachedSubmissionFolder}.
   */
  public abstract boolean hasItemsInDatabase();

  public abstract NetworkCallState loadFromRemoteState();

  public static SubmissionsDatabaseAndNetworkState create(boolean hasItemsInDatabase, NetworkCallState loadFromRemoteState) {
    return new AutoValue_SubmissionsDatabaseAndNetworkState(hasItemsInDatabase, loadFromRemoteState);
  }
}
