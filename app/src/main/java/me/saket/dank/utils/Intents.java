package me.saket.dank.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import android.support.v4.app.ShareCompat;

import me.saket.dank.R;

/**
 * Utility methods related to Intents.
 */
public class Intents {

  /**
   * For opening an Url in the browser.
   */
  @CheckResult
  public static Intent createForOpeningUrl(String url) {
    return new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url));
  }

  /**
   * For sharing an Url and its title. Not all apps support reading the title though.
   */
  @CheckResult
  public static Intent createForSharingUrl(Context context, String url) {
    return createForSharingUrl(context, null, url);
  }

  /**
   * For sharing an Url and its subject. Not all apps support reading the subject though.
   */
  @CheckResult
  public static Intent createForSharingUrl(Context context, @Nullable String subject, String url) {
    Intent shareIntent = new Intent(Intent.ACTION_SEND);
    shareIntent.setType("text/plain");
    if (subject != null && !subject.isEmpty()) {
      shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
    }
    shareIntent.putExtra(Intent.EXTRA_TEXT, url);
    return Intent.createChooser(shareIntent, context.getString(R.string.common_share_sheet_title));
  }

  @CheckResult
  public static Intent createForSharingMedia(Context context, Uri mediaContentUri) {
    return new Intent().setAction(Intent.ACTION_SEND)
        .putExtra(ShareCompat.EXTRA_CALLING_PACKAGE, context.getPackageName())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        .putExtra(Intent.EXTRA_STREAM, mediaContentUri)
        .setType(context.getContentResolver().getType(mediaContentUri))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
  }

  @CheckResult
  public static Intent createForViewingMedia(Context context, Uri mediaContentUri) {
    return new Intent().setAction(Intent.ACTION_VIEW)
        .putExtra(ShareCompat.EXTRA_CALLING_PACKAGE, context.getPackageName())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        .putExtra(Intent.EXTRA_STREAM, mediaContentUri)
        .setType(context.getContentResolver().getType(mediaContentUri))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
  }
}
