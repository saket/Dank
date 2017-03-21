package me.saket.dank.utils;

/**
 * Utility methods that don't fit in anywhere.
 */
public class CommonUtils {

    public static <T> T defaultIfNull(T valueToCheck, T defaultValue) {
        return valueToCheck != null ? valueToCheck : defaultValue;
    }

}
