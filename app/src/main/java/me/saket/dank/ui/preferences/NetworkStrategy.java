package me.saket.dank.ui.preferences;

import android.app.job.JobInfo;
import androidx.annotation.StringRes;

import me.saket.dank.R;

public enum NetworkStrategy {
  WIFI_ONLY(R.string.userprefs_networkstrategy_only_on_wifi) {
    @Override
    public JobInfo.Builder setNetworkRequirement(JobInfo.Builder jobBuilder) {
      return jobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED);
    }
  },
  WIFI_OR_MOBILE_DATA(R.string.userprefs_networkstrategy_wifi_or_mobile_data) {
    @Override
    public JobInfo.Builder setNetworkRequirement(JobInfo.Builder jobBuilder) {
      return jobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
    }
  },
  NEVER(R.string.userprefs_networkstrategy_never) {
    @Override
    public JobInfo.Builder setNetworkRequirement(JobInfo.Builder jobBuilder) {
      return jobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE);
    }
  },
  ;

  @StringRes
  public final int displayNameRes;

  NetworkStrategy(@StringRes int displayNameRes) {
    this.displayNameRes = displayNameRes;
  }

  public JobInfo.Builder setNetworkRequirement(JobInfo.Builder jobBuilder) {
    throw new AbstractMethodError();
  }
}
