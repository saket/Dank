package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.TimePeriod;

import java.io.Serializable;

/**
 * Uniquely identifies a cached submission by its subreddit's name and sorting information.
 */
@AutoValue
public abstract class CachedSubmissionFolder implements Serializable {

  private static final String SEPARATOR = "____";

  public abstract String subredditName();

  public abstract SortingAndTimePeriod sortingAndTimePeriod();

  public static CachedSubmissionFolder create(String subredditName, Sorting sortOrder) {
    return create(subredditName, SortingAndTimePeriod.create(sortOrder));
  }

  public static CachedSubmissionFolder create(String subredditName, SortingAndTimePeriod sortingAndTimePeriod) {
    return new AutoValue_CachedSubmissionFolder(subredditName, sortingAndTimePeriod);
  }

  /**
   * Used for storing this into DB. Not using moshi because this will be searcheable.
   */
  public String serialize() {
    return subredditName() + SEPARATOR + sortingAndTimePeriod().sortOrder().name() + SEPARATOR + sortingAndTimePeriod().timePeriod().name();
  }

  public static CachedSubmissionFolder valueOf(String serializedSrtingAndTimePeriod) {
    String[] parts = serializedSrtingAndTimePeriod.split(SEPARATOR);
    String subredditName = parts[0];
    Sorting sorting = Sorting.valueOf(parts[1]);
    TimePeriod sortTimePeriod = TimePeriod.valueOf(parts[2]);

    return create(subredditName, SortingAndTimePeriod.create(sorting, sortTimePeriod));
  }
}
