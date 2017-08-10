package me.saket.dank.utils;

import android.content.res.Resources;
import android.text.format.DateUtils;
import me.saket.dank.R;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;

public class Dates extends DateUtils {

  /**
   * Because DateUtils.getRelativeTimeSpanString() does not support "just now".
   */
  public static CharSequence createTimestamp(Resources resources, long timeMillis) {
    if (System.currentTimeMillis() - timeMillis < MINUTE_IN_MILLIS) {
      return resources.getString(R.string.timestamp_just_now);
    } else {
      return getRelativeTimeSpanString(timeMillis, System.currentTimeMillis(), 0, FORMAT_ABBREV_RELATIVE | FORMAT_ABBREV_MONTH);
    }
  }
}
