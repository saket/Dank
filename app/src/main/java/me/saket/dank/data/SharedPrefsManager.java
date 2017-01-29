package me.saket.dank.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.UUID;

public class SharedPrefsManager {

    private static final String KEY_DEVICE_UUID = "deviceUuid";

    private SharedPreferences sharedPrefs;

    public SharedPrefsManager(Context context) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public UUID getDeviceUuid() {
        if (!sharedPrefs.contains(KEY_DEVICE_UUID)) {
            sharedPrefs.edit()
                    .putString(KEY_DEVICE_UUID, UUID.randomUUID().toString())
                    .apply();
        }
        return UUID.fromString(sharedPrefs.getString(KEY_DEVICE_UUID, null));
    }

}
