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
    public static Intent createForUrl(String url) {
        return new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url));
    }

}
