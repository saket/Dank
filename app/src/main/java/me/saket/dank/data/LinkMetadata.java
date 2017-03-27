package me.saket.dank.data;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.auto.value.AutoValue;

import me.saket.dank.utils.UrlMetadataParser;

/**
 * Details of a URL, parsed by {@link UrlMetadataParser}.
 */
@AutoValue
public abstract class LinkMetadata {

    public abstract String url();

    @Nullable
    public abstract String title();

    @Nullable
    public abstract String faviconUrl();

    @Nullable
    public abstract String imageUrl();

    public boolean hasImage() {
        return !TextUtils.isEmpty(imageUrl());
    }

    public boolean hasFavicon() {
        return !TextUtils.isEmpty(faviconUrl());
    }

    public static LinkMetadata create(String url, @Nullable String title, @Nullable String faviconUrl, @Nullable String imageUrl) {
        return new AutoValue_LinkMetadata(url, title, faviconUrl, imageUrl);
    }

}
