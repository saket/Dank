package me.saket.dank.ui.preferences;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import me.saket.dank.R;
import me.saket.dank.ui.webview.ChromeCustomTabsForwardingActivity;
import me.saket.dank.ui.webview.WebViewActivity;
import me.saket.dank.utils.Intents;

public enum DefaultWebBrowser {
  DANK_INTERNAL_BROWSER(R.string.userprefs_externallinks_internal_browser) {
    @Override
    public Intent intentForUrl(Context context, String url, @Nullable Rect expandFromRect) {
      return WebViewActivity.intent(context, url, expandFromRect);
    }
  },
  CHROME_CUSTOM_TABS(R.string.userprefs_externallinks_chrome_custom_tabs) {
    @Override
    public Intent intentForUrl(Context context, String url, @Nullable Rect expandFromRect) {
      return ChromeCustomTabsForwardingActivity.intent(context, url);
    }
  },
  DEVICE_DEFAULT(R.string.userprefs_externallinks_default_web_browser) {
    @Override
    public Intent intentForUrl(Context context, String url, @Nullable Rect expandFromRect) {
      return Intents.createForOpeningUrl(url);
    }
  };

  @StringRes
  public final int displayName;

  DefaultWebBrowser(@StringRes int displayName) {
    this.displayName = displayName;
  }

  public Intent intentForUrl(Context context, String url, @Nullable Rect expandFromRect) {
    throw new AbstractMethodError();
  }
}
