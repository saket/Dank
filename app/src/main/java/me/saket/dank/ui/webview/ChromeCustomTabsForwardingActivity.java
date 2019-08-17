package me.saket.dank.ui.webview;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import me.saket.dank.R;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.ui.UrlRouter;

/**
 * This activity exists because {@link UrlRouter.IntentRouter} needs to return an intent to the caller.
 * UrlRouter isn't designed to handle returning both an Intent and ChromeCustomTabs's animations.
 */
public class ChromeCustomTabsForwardingActivity extends DankActivity {

  private static final String KEY_URL = "url";

  public static Intent intent(Context context, String url) {
    return new Intent(context, ChromeCustomTabsForwardingActivity.class)
        .putExtra(KEY_URL, url);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder()
        .addDefaultShareMenuItem()
        .enableUrlBarHiding()
        .setToolbarColor(ContextCompat.getColor(this, R.color.color_primary_dark))
        .setStartAnimations(this, R.anim.chromecustomtab_enter_from_bottom, R.anim.nothing)
        .setExitAnimations(this, R.anim.nothing, R.anim.chromecustomtab_exit_to_bottom);

    CustomTabsIntent customTabsIntent = builder.build();

    String url = getIntent().getStringExtra(KEY_URL);
    customTabsIntent.launchUrl(this, Uri.parse(url));

    finish();
  }
}
