package me.saket.dank.ui.submission;

import android.os.Parcelable;
import android.support.annotation.StringRes;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import net.dean.jraw.models.Sorting;
import net.dean.jraw.models.SubredditSort;
import net.dean.jraw.models.TimePeriod;

import java.io.Serializable;

import me.saket.dank.R;

@AutoValue
public abstract class SortingAndTimePeriod implements Parcelable, Serializable {

  private static final String SEPARATOR = "____";

  public abstract SubredditSort sortOrder();

  public abstract TimePeriod timePeriod();

  public static SortingAndTimePeriod create(SubredditSort sortOrder) {
    if (sortOrder.getRequiresTimePeriod()) {
      throw new AssertionError(sortOrder + " requires a time-period");
    }
    return create(sortOrder, TimePeriod.DAY /* using a dummy period instead of a null */);
  }

  public static SortingAndTimePeriod create(SubredditSort sortOrder, TimePeriod timePeriod) {
    return new AutoValue_SortingAndTimePeriod(sortOrder, timePeriod);
  }

  public String serialize() {
    return sortOrder().getName() + SEPARATOR + timePeriod().name();
  }

  @StringRes
  public int getSortingDisplayTextRes() {
    switch (sortOrder()) {
      case HOT:
        return R.string.sorting_mode_hot;

      case BEST:
        return R.string.sorting_mode_best;

      case NEW:
        return R.string.sorting_mode_new;

      case RISING:
        return R.string.sorting_mode_rising;

      case CONTROVERSIAL:
        return R.string.sorting_mode_controversial;

      case TOP:
        return R.string.sorting_mode_top;

      default:
        throw new UnsupportedOperationException("Unknown sorting: " + sortOrder());
    }
  }

  @StringRes
  public int getTimePeriodDisplayTextRes() {
    switch (timePeriod()) {
      case HOUR:
        return R.string.sorting_time_period_hour;

      case DAY:
        return R.string.sorting_time_period_day;

      case WEEK:
        return R.string.sorting_time_period_week;

      case MONTH:
        return R.string.sorting_time_period_month;

      case YEAR:
        return R.string.sorting_time_period_year;

      case ALL:
        return R.string.sorting_time_period_alltime;

      default:
        throw new UnsupportedOperationException("Unknown time period: " + timePeriod());
    }
  }

  public static JsonAdapter<SortingAndTimePeriod> jsonAdapter(Moshi moshi) {
    return new AutoValue_SortingAndTimePeriod.MoshiJsonAdapter(moshi);
  }
}
