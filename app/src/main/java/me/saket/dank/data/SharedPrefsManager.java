package me.saket.dank.data;

import android.content.SharedPreferences;

import java.util.UUID;

public class SharedPrefsManager {

  private static final String KEY_DEVICE_UUID = "deviceUuid";
  private static final String KEY_UNREAD_FOLDER_ACTIVE = "unreadFolderActive";

  protected SharedPreferences sharedPrefs;

  public SharedPrefsManager(SharedPreferences sharedPrefs) {
    this.sharedPrefs = sharedPrefs;
  }

  public UUID getDeviceUuid() {
    if (!sharedPrefs.contains(KEY_DEVICE_UUID)) {
      sharedPrefs.edit()
          .putString(KEY_DEVICE_UUID, UUID.randomUUID().toString())
          .apply();
    }
    return UUID.fromString(sharedPrefs.getString(KEY_DEVICE_UUID, null));
  }

  public void setUnreadMessagesFolderActive(boolean unreadActive) {
    sharedPrefs.edit().putBoolean(KEY_UNREAD_FOLDER_ACTIVE, unreadActive).apply();
  }

  public boolean isUnreadMessagesFolderActive(boolean defaultValue) {
    return sharedPrefs.getBoolean(KEY_UNREAD_FOLDER_ACTIVE, defaultValue);
  }
}
