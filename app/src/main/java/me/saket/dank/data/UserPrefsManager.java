package me.saket.dank.data;

/**
 * Used for accessing user's preferences.
 */
public class UserPrefsManager {

    private SharedPrefsManager sharedPrefsManager;

    public UserPrefsManager(SharedPrefsManager sharedPrefsManager) {
        this.sharedPrefsManager = sharedPrefsManager;
    }

    public String defaultSubreddit() {
        return null;
    }

    public void setDefaultSubreddit(String subredditName) {

    }

}
