package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.TimePeriod;

/**
 * Uniquely identifies a cached submission by its subreddit's name and sorting information.
 */
@AutoValue
public abstract class CachedSubmissionFolder {

  private static final String SEPARATOR = "____";

  public abstract String subredditName();

  public abstract Sorting sortOrder();

  public abstract TimePeriod sortTimePeriod();

  public static CachedSubmissionFolder create(String subredditName, Sorting sortOrder, TimePeriod timePeriod) {
    return new AutoValue_CachedSubmissionFolder(subredditName, sortOrder, timePeriod);
  }

  public static CachedSubmissionFolder create(String subredditName, Sorting sortOrder) {
    if (sortOrder.requiresTimePeriod()) {
      throw new AssertionError("Sorting requires a timeperiod");
    }
    return create(subredditName, sortOrder, TimePeriod.DAY /* using a dummy period of a null value. */);
  }

  /**
   * Used for storing this into DB. Not using moshi because this will be searcheable.
   */
  public String serialize() {
    return subredditName() + SEPARATOR + sortOrder().name() + SEPARATOR + sortTimePeriod().name();
  }

  public static CachedSubmissionFolder valueOf(String serializedSrtingAndTimePeriod) {
    String[] parts = serializedSrtingAndTimePeriod.split(SEPARATOR);
    String subredditName = parts[0];
    Sorting sorting = Sorting.valueOf(parts[1]);
    TimePeriod sortTimePeriod = TimePeriod.valueOf(parts[2]);

    return CachedSubmissionFolder.create(subredditName, sorting, sortTimePeriod);
  }
}
