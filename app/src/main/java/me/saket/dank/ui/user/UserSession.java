package me.saket.dank.ui.user;

import android.content.SharedPreferences;

public class UserSession {

  private static final String KEY_LOGGED_IN_USERNAME = "loggedInUsername";
  private SharedPreferences sharedPrefs;

  public UserSession(SharedPreferences sharedPrefs) {
    this.sharedPrefs = sharedPrefs;
  }

  public void setLoggedInUsername(String username) {
    sharedPrefs.edit().putString(KEY_LOGGED_IN_USERNAME, username).apply();
  }

  public boolean isUserLoggedIn() {
    return loggedInUserName() != null;
  }

  public String loggedInUserName() {
    return sharedPrefs.getString(KEY_LOGGED_IN_USERNAME, null);
  }
}
