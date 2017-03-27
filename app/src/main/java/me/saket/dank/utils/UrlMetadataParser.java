package me.saket.dank.utils;

import static android.text.TextUtils.isEmpty;

import android.net.Uri;
import android.support.annotation.CheckResult;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import me.saket.dank.data.LinkMetadata;
import me.saket.dank.di.Dank;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import rx.Single;

/**
 * Extracts information of a URL.
 */
public class UrlMetadataParser {

    private static final String CHROME_DESKTOP_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/56.0.2924.87 Safari/537.36";

    /**
     * @param ignoreSocialData When true, facebook/twitter titles, images will be ignored and the page HTML title will be used instead.
     */
    @CheckResult
    public static Single<LinkMetadata> parse(String url, boolean ignoreSocialData) {
        return Single.fromCallable(() -> {
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", CHROME_DESKTOP_USER_AGENT)
                    .build();

            Call call = Dank.okHttpClient().newCall(request);
            try (Response response = call.execute()) {
                Document pageDocument = Jsoup.parse(response.body().string(), url);
                return extractMetaData(url, pageDocument, ignoreSocialData);
            }
        });
    }

    private static LinkMetadata extractMetaData(String url, Document pageDocument, boolean ignoreSocialData) {
        String linkTitle;
        String linkImage;
        String faviconUrl;

        if (ignoreSocialData) {
            return LinkMetadata.create(url, pageDocument.title(), null, null);
        }

        // Websites seem to give better images for twitter, maybe because Twitter shows smaller
        // thumbnails than Facebook. So we'll prefer Twitter over Facebook.

        // Thumbnail.
        boolean isGooglePlayLink = isGooglePlayLink(url);
        if (isGooglePlayLink) {
            // Play Store uses a shitty favicon.
            linkImage = "https://www.android.com/static/2016/img/icons/why-android/play_2x.png";

        } else {
            linkImage = getMetaTag(pageDocument, "twitter:image", true);
            if (isEmpty(linkImage)) {
                linkImage = getMetaTag(pageDocument, "og:image", true);
            }
            if (isEmpty(linkImage)) {
                linkImage = getMetaTag(pageDocument, "twitter:image:src", true);
            }
            if (isEmpty(linkImage)) {
                linkImage = getMetaTag(pageDocument, "og:image:secure_url", true);
            }

            // So... scheme-less URLs are also a thing.
            if (linkImage != null && linkImage.startsWith("//")) {
                Uri imageURI = Uri.parse(url);
                linkImage = imageURI.getScheme() + linkImage;
            }
        }

        // Title.
        linkTitle = getMetaTag(pageDocument, "twitter:title", false);
        if (isEmpty(linkTitle)) {
            linkTitle = getMetaTag(pageDocument, "og:title", false);
        }
        if (isEmpty(linkTitle)) {
            linkTitle = pageDocument.title();
        }

        // Favicon.
        faviconUrl = getLinkRelTag(pageDocument, "apple-touch-icon");
        if (isEmpty(faviconUrl)) {
            faviconUrl = getLinkRelTag(pageDocument, "apple-touch-icon-precomposed");
        }
        if (isEmpty(faviconUrl)) {
            faviconUrl = getLinkRelTag(pageDocument, "shortcut icon");
        }
        if (isEmpty(faviconUrl)) {
            faviconUrl = getLinkRelTag(pageDocument, "icon");
        }
        if (isEmpty(faviconUrl)) {
            if (!isGooglePlayLink) {
                // Thanks Google for the backup!
                faviconUrl = "https://www.google.com/s2/favicons?domain_url=" + url;

            } else {
                // Play Store uses a shitty favicon. Prefer the thumbnail instead.
                faviconUrl = null;
            }
        }

        return LinkMetadata.create(url, linkTitle, faviconUrl, linkImage);
    }

    private static String getMetaTag(Document document, String attr, boolean useAbsoluteUrl) {
        Elements elements = document.select("meta[name=" + attr + "]");
        for (Element element : elements) {
            final String url = element.attr(useAbsoluteUrl ? "abs:content" : "content");
            if (url != null) {
                return url;
            }
        }
        elements = document.select("meta[property=" + attr + "]");
        for (Element element : elements) {
            final String url = element.attr(useAbsoluteUrl ? "abs:content" : "content");
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    private static String getLinkRelTag(Document document, String rel) {
        Elements elements = document.head().select("link[rel=" + rel + "]");
        if (elements.isEmpty()) {
            return null;
        }

        String largestSizeUrl = elements.first().attr("abs:href");
        int largestSize = 0;

        for (Element element : elements) {
            // Some websites have multiple icons for different sizes. Find the largest one.
            String sizes = element.attr("sizes");
            int size;
            if (sizes.contains("x")) {
                size = Integer.parseInt(sizes.split("x")[0]);

                if (size > largestSize) {
                    largestSize = size;
                    largestSizeUrl = element.attr("abs:href");
                }
            }
        }
        return largestSizeUrl;
    }

    public static boolean isGooglePlayLink(String url) {
        return Uri.parse(url).getHost().contains("play.google");
    }

}
