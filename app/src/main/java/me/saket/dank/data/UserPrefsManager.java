package me.saket.dank.data;

import android.content.SharedPreferences;

/**
 * Used for accessing user's preferences.
 */
public class UserPrefsManager {

    private static final String KEY_DEFAULT_SUBREDDIT = "defaultSubreddit";

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

}
