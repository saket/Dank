package me.saket.dank.utils;

import android.os.PersistableBundle;

public class PersistableBundleUtils {

  /**
   * Because {@link PersistableBundle} added support for putting booleans only in API 22.
   */
  public static void putBoolean(PersistableBundle bundle, String key, boolean value) {
    bundle.putString(key, String.valueOf(value));
  }

  public static boolean getBoolean(PersistableBundle bundle, String key) {
    return Boolean.parseBoolean(bundle.getString(key));
  }

}
