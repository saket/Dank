package me.saket.dank.data;

import android.support.annotation.StringRes;
import me.saket.dank.R;

public enum NetworkStrategy {
  WIFI_ONLY(R.string.userprefs_networkstrategy_only_on_wifi),
  WIFI_OR_MOBILE_DATA(R.string.userprefs_networkstrategy_wifi_or_mobile_data),
  NEVER(R.string.userprefs_networkstrategy_never),
  ;

  public final int displayNameRes;

  NetworkStrategy(@StringRes int displayNameRes) {
    this.displayNameRes = displayNameRes;
  }
}
