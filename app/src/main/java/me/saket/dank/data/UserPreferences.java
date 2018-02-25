package me.saket.dank.data;

import android.content.SharedPreferences;
import android.text.format.DateUtils;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Used for accessing user's preferences.
 */
@Singleton
public class UserPreferences {

  private static final long DEFAULT_INTERVAL_FOR_MESSAGES_CHECK_MILLIS = DateUtils.MINUTE_IN_MILLIS * 30;

  private static final String KEY_DEFAULT_SUBREDDIT = "defaultSubreddit";
  private static final String KEY_UNREAD_MESSAGES_CHECK_INTERVAL_MILLIS = "unreadMessagesCheckInterval";
  public static final String KEY_HIGH_RESOLUTION_MEDIA_NETWORK_STRATEGY = "high_resolution_media_network_strategy";

  private final SharedPreferences sharedPrefs;

  @Inject
  public UserPreferences(SharedPreferences sharedPrefs) {
    this.sharedPrefs = sharedPrefs;
  }

  public String defaultSubreddit(String valueIfNull) {
    return sharedPrefs.getString(KEY_DEFAULT_SUBREDDIT, valueIfNull);
  }

  public void setDefaultSubreddit(String subredditName) {
    sharedPrefs.edit().putString(KEY_DEFAULT_SUBREDDIT, subredditName).apply();
  }

  public long unreadMessagesCheckIntervalMillis() {
    return sharedPrefs.getLong(KEY_UNREAD_MESSAGES_CHECK_INTERVAL_MILLIS, DEFAULT_INTERVAL_FOR_MESSAGES_CHECK_MILLIS);
  }
}
