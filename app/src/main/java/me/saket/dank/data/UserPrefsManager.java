package me.saket.dank.data;

import android.content.SharedPreferences;
import android.text.format.DateUtils;

/**
 * Used for accessing user's preferences.
 */
public class UserPrefsManager {

  private static final long DEFAULT_INTERVAL_FOR_MESSAGES_CHECK_MILLIS = DateUtils.MINUTE_IN_MILLIS * 30;

  private static final String KEY_DEFAULT_SUBREDDIT = "defaultSubreddit";
  private static final String KEY_UNREAD_MESSAGES_CHECK_INTERVAL_MILLIS = "unreadMessagesCheckInterval";
  private static final String KEY_SHOW_SUBMISSION_COMMENTS_COUNT_IN_BYLINE = "showSubmissionCommentsCountInByline";

  private static final boolean DEFAULT_VALUE_SHOW_SUBMISSION_COMMENTS_COUNT = false;

  private SharedPreferences sharedPrefs;

  public UserPrefsManager(SharedPreferences sharedPrefs) {
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

  public void setShowSubmissionCommentsCountInByline(boolean showCommentsCount) {
    sharedPrefs.edit().putBoolean(KEY_SHOW_SUBMISSION_COMMENTS_COUNT_IN_BYLINE, showCommentsCount).apply();
  }

  public boolean canShowSubmissionCommentsCountInByline() {
    return sharedPrefs.getBoolean(KEY_SHOW_SUBMISSION_COMMENTS_COUNT_IN_BYLINE, DEFAULT_VALUE_SHOW_SUBMISSION_COMMENTS_COUNT);
  }
}
