package me.saket.dank.utils;

import android.net.Uri;

import timber.log.Timber;

/**
 * Utility methods for URLs.
 */
public class Urls {

    /**
     * Gets the domain name without any TLD. For example, this will return "nytimes.com" when
     * <var>url</var> is "http://www.nytimes.com/2016/11/30/technology/while...".
     */
    public static String parseDomainName(String url) {
        String domainName = null;

        try {
            // getHost() returns the part between http(s):// and the first slash.
            // So this will contain the TLD and possibly a "www".
            domainName = Uri.parse(url).getHost();

            if (domainName.startsWith("www.")) {
                domainName = domainName.substring(4);
            }

        } catch (Exception e) {
            Timber.e(e, "Error while parsing domain name from URL: %s", url);
        }

        return domainName;
    }

}
