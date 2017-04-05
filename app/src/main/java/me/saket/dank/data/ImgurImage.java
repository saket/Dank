package me.saket.dank.data;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class ImgurImage {

    @Nullable
    @Json(name = "title")
    public abstract String title();

    @Nullable
    @Json(name = "description")
    public abstract String description();

    @Nullable
    @Json(name = "mp4")
    abstract String videoLink();

    @Nullable
    @Json(name = "link")
    abstract String staticImageLink();

    /**
     * Null when {@link #staticImageLink()} is available.
     */
    @Nullable
    @Json(name = "hash")
    abstract String hash();

    /**
     * Null when {@link #staticImageLink()} is available.
     */
    @Nullable
    @Json(name = "ext")
    abstract String imageFormat();

    public String url() {
        if (videoLink() != null) {
            return videoLink();

        } else if (staticImageLink() != null) {
            // link is only present in the paid API's response.
            return staticImageLink();

        } else {
            return "https://i.imgur.com/" + hash() + imageFormat();
        }
    }

    public static JsonAdapter<ImgurImage> jsonAdapter(Moshi moshi) {
        return new AutoValue_ImgurImage.MoshiJsonAdapter(moshi);
    }

}
