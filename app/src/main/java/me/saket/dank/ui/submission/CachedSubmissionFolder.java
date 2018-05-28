package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.SubredditSort;
import net.dean.jraw.models.TimePeriod;

import java.io.Serializable;

/**
 * Uniquely identifies a cached submission by its subreddit's name and sorting information.
 */
@AutoValue
public abstract class CachedSubmissionFolder implements Serializable {

  private static final String SEPARATOR = "____";

  // Why do we also need the subreddit name here?
  public abstract String subredditName();

  public abstract SortingAndTimePeriod sortingAndTimePeriod();

  public static CachedSubmissionFolder create(String subredditName, SubredditSort sortOrder) {
    return create(subredditName, SortingAndTimePeriod.create(sortOrder));
  }

  public static CachedSubmissionFolder create(String subredditName, SortingAndTimePeriod sortingAndTimePeriod) {
    return new AutoValue_CachedSubmissionFolder(subredditName, sortingAndTimePeriod);
  }

  /**
   * Used for storing this into DB. Not using moshi because this will be searcheable.
   */
  public String serialize() {
    return subredditName() + SEPARATOR + sortingAndTimePeriod().serialize();
  }

  public static CachedSubmissionFolder valueOf(String serializedSrtingAndTimePeriod) {
    String[] parts = serializedSrtingAndTimePeriod.split(SEPARATOR);
    String subredditName = parts[0];
    SubredditSort sorting = SubredditSort.valueOf(parts[1]);
    TimePeriod sortTimePeriod = TimePeriod.valueOf(parts[2]);

    return create(subredditName, SortingAndTimePeriod.create(sorting, sortTimePeriod));
  }
}
