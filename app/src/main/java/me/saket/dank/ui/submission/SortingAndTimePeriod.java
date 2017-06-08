package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.TimePeriod;

import java.io.Serializable;

@AutoValue
public abstract class SortingAndTimePeriod implements Serializable {

  public abstract Sorting sortOrder();

  public abstract TimePeriod timePeriod();

  public static SortingAndTimePeriod create(Sorting sortOrder) {
    if (sortOrder.requiresTimePeriod()) {
      throw new AssertionError(sortOrder + " requires a time-period");
    }
    return create(sortOrder, TimePeriod.DAY /* using a dummy period instead of a null */);
  }

  public static SortingAndTimePeriod create(Sorting sortOrder, TimePeriod timePeriod) {
    return new AutoValue_SortingAndTimePeriod(sortOrder, timePeriod);
  }
}
