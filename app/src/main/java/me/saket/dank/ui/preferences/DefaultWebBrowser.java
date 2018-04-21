package me.saket.dank.ui.preferences;

import android.support.annotation.StringRes;

import me.saket.dank.R;

public enum DefaultWebBrowser {
  DANK_INTERNAL_BROWSER(R.string.userprefs_externallinks_internal_browser),
  CHROME_CUSTOM_TABS(R.string.userprefs_externallinks_chrome_custom_tabs),
  DEVICE_DEFAULT(R.string.userprefs_externallinks_default_web_browser),;

  public final int displayName;

  DefaultWebBrowser(@StringRes int displayName) {
    this.displayName = displayName;
  }
}
