package me.saket.dank.utils;

import android.support.annotation.CheckResult;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
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

    @CheckResult
    public static Single<LinkMetadata> parse(String url) {
        return Single.fromCallable(() -> {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Call call = Dank.okHttpClient().newCall(request);
            try (Response response = call.execute()) {
                return extractMetaData(url, response.body().string());
            }
        });
    }

    private static LinkMetadata extractMetaData(String url, String pageHtml) {
        Document pageDocument = Jsoup.parse(pageHtml, url);
        String linkTitle = null;
        String linkImage = null;

        // Extract Open Graph data. If no properties found, infer from existing information
        for (Element property : pageDocument.select("meta[property^=og:]")) {
            switch (property.attr("property")) {
                case "og:title":
                    linkTitle = property.attr("content");
                    break;

                case "og:image":
                    linkImage = property.attr("abs:content");
                    break;
            }
        }

        // Fallback to <title> if og:title was empty.
        if (StringUtil.isBlank(linkTitle)) {
            linkTitle = pageDocument.title();
        }

        Elements faviconLinkElements = pageDocument.head().select("link[rel=shortcut icon]");

        String faviconUrl = null;
        for (Element element : faviconLinkElements) {
            if (element.hasAttr("href")) {
                faviconUrl = element.absUrl("href");
            }
        }

        return LinkMetadata.create(linkTitle, faviconUrl, linkImage);
    }

}
