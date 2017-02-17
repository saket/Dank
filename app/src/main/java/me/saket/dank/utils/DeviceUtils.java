package me.saket.dank.utils;

import android.os.Build;

/**
 * Utility methods related to the user's device.
 */
public class DeviceUtils {

    public static boolean isEmulator() {
        return Build.PRODUCT.startsWith("sdk_google_phone");
    }

}
