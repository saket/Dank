package me.saket.dank.ui.submission

import android.os.Parcelable
import androidx.annotation.StringRes
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import me.saket.dank.R
import net.dean.jraw.models.SubredditSort
import net.dean.jraw.models.SubredditSort.*
import net.dean.jraw.models.TimePeriod
import net.dean.jraw.models.TimePeriod.*

@Parcelize
@JsonClass(generateAdapter = true)
data class SortingAndTimePeriod(
    @get:JvmName("sortOrder")
    val sortOrder: SubredditSort,

    @get:JvmName("timePeriod")
    val timePeriod: TimePeriod
) : Parcelable {

  constructor(sortOrder: SubredditSort) : this(sortOrder, DAY) {
    if (sortOrder.requiresTimePeriod) {
      throw AssertionError("$sortOrder requires a time-period")
    }
  }

  @StringRes
  fun sortingDisplayTextRes(): Int {
    return when (sortOrder) {
      HOT -> R.string.sorting_mode_hot
      BEST -> R.string.sorting_mode_best
      NEW -> R.string.sorting_mode_new
      RISING -> R.string.sorting_mode_rising
      CONTROVERSIAL -> R.string.sorting_mode_controversial
      TOP -> R.string.sorting_mode_top
      else -> throw UnsupportedOperationException("Unknown sorting: $sortOrder")
    }
  }

  @StringRes
  fun timePeriodDisplayTextRes(): Int {
    return when (timePeriod) {
      HOUR -> R.string.sorting_time_period_hour
      DAY -> R.string.sorting_time_period_day
      WEEK -> R.string.sorting_time_period_week
      MONTH -> R.string.sorting_time_period_month
      YEAR -> R.string.sorting_time_period_year
      ALL -> R.string.sorting_time_period_alltime
      else -> throw UnsupportedOperationException("Unknown time period: $timePeriod")
    }
  }
}

