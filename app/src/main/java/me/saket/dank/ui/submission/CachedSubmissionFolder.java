package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.SubredditSort;

import java.io.Serializable;

/**
 * Uniquely identifies a cached submission by its subreddit's name and sorting information.
 */
@AutoValue
public abstract class CachedSubmissionFolder implements Serializable {

  // Why do we also need the subreddit name here?
  public abstract String subredditName();

  public abstract SortingAndTimePeriod sortingAndTimePeriod();

  /**
   * Remove `@JvmOverloads` from [SortingAndTimePeriod].
   */
  public static CachedSubmissionFolder create(String subredditName, SubredditSort sortOrder) {
    return create(subredditName, new SortingAndTimePeriod(sortOrder));
  }

  public static CachedSubmissionFolder create(String subredditName, SortingAndTimePeriod sortingAndTimePeriod) {
    return new AutoValue_CachedSubmissionFolder(subredditName, sortingAndTimePeriod);
  }
}
