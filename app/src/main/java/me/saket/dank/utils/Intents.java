package me.saket.dank.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Utility methods related to Intents.
 */
public class Intents {

  /**
   * Check if there are any installed app(s) that can handle <var>intent</var>.
   *
   * @return True if any app is present that can handle the intent. False otherwise.
   */
  public static boolean hasAppToHandleIntent(Context context, Intent intent) {
    return intent.resolveActivity(context.getPackageManager()) != null;
  }

  /**
   * Create an Intent for opening an Url in the browser.
   */
  public static Intent createForOpeningUrl(String url) {
    return new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url));
  }

  /**
   * Create an Intent for sharing an Url and its title. Not all apps support reading the title though.
   */
  public static Intent createForSharingUrl(String title, String url) {
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.setType("text/plain");
    intent.putExtra(Intent.EXTRA_TEXT, url);
    intent.putExtra(Intent.EXTRA_SUBJECT, title);
    return intent;
  }

}
