package me.saket.dank.data;

import com.google.auto.value.AutoValue;

import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.TimePeriod;

import me.saket.dank.ui.submission.CachedSubmission;

/**
 * Stores a subreddit's sorting information. This class exists so that the sorting info can be
 * cached in {@link CachedSubmission}.
 */
@AutoValue
public abstract class SortingAndTimePeriod {

  public abstract Sorting sortOrder();

  public abstract TimePeriod timePeriod();

  public static SortingAndTimePeriod create(Sorting sortOrder, TimePeriod timePeriod) {
    return new AutoValue_SortingAndTimePeriod(sortOrder, timePeriod);
  }

  /**
   * Used for storing this into DB. Not using moshi because this will be searcheable.
   */
  public String serialize() {
    return sortOrder().name() + "_" + timePeriod().name();
  }

  public static SortingAndTimePeriod valueOf(String serializedSrtingAndTimePeriod) {
    String[] parts = serializedSrtingAndTimePeriod.split("_");
    return SortingAndTimePeriod.create(Sorting.valueOf(parts[0]), TimePeriod.valueOf(parts[1]));
  }
}
