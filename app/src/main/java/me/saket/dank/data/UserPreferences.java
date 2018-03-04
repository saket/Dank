package me.saket.dank.data;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Used for accessing user's preferences.
 */
@Singleton
public class UserPreferences {

  private static final String KEY_DEFAULT_SUBREDDIT = "defaultSubreddit";

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
}
