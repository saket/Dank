package me.saket.dank.utils;

import android.text.Html;

import net.dean.jraw.models.Thumbnails;

/**
 * Utility methods that don't fit in anywhere.
 */
public class CommonUtils {

    public static <T> T defaultIfNull(T valueToCheck, T defaultValue) {
        return valueToCheck != null ? valueToCheck : defaultValue;
    }

    /**
     * Find a thumbnail provided by Reddit that is the closest to <var>optimizeForWidth</var>.
     * Gives preference to higher-res thumbnails if needed.
     */
    public static String findOptimizedImage(Thumbnails redditSuppliedImages, int optimizeForWidth) {
        Thumbnails.Image closestImage = redditSuppliedImages.getSource();
        int closestDifference = optimizeForWidth - redditSuppliedImages.getSource().getWidth();

        for (Thumbnails.Image redditCopy : redditSuppliedImages.getVariations()) {
            int differenceAbs = Math.abs(optimizeForWidth - redditCopy.getWidth());

            if (differenceAbs < Math.abs(closestDifference)
                    // If another image is found with the same difference, choose the higher-res image.
                    || differenceAbs == closestDifference && redditCopy.getWidth() > closestImage.getWidth())
            {
                closestDifference = optimizeForWidth - redditCopy.getWidth();
                closestImage = redditCopy;
            }
        }

        // Reddit sends HTML-escaped URLs.
        //noinspection deprecation
        return Html.fromHtml(closestImage.getUrl()).toString();
    }

}
