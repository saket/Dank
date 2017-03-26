package me.saket.dank.data;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import me.saket.dank.utils.UrlMetadataParser;

/**
 * Details of a URL, parsed by {@link UrlMetadataParser}.
 */
@AutoValue
public abstract class LinkMetadata {

    @Nullable
    public abstract String title();

    @Nullable
    public abstract String faviconUrl();

    @Nullable
    public abstract String imageUrl();

    public static LinkMetadata create(@Nullable String title, @Nullable String faviconUrl, @Nullable String imageUrl) {
        return new AutoValue_LinkMetadata(title, faviconUrl, imageUrl);
    }

}
