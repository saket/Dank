package me.saket.dank.data;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import me.saket.dank.di.DankApi;

/**
 * API response body of {@link DankApi#imgurImagePaid(String)}.
 */
@AutoValue
public abstract class ImgurImageResponse implements ImgurResponse {

    @Json(name = "data")
    abstract ImgurImage image();

    @Override
    @Json(name = "success")
    public abstract boolean hasImages();

    @Override
    public boolean isAlbum() {
        return false;
    }

    @Override
    public List<ImgurImage> images() {
        return Collections.singletonList(image());
    }

    public static JsonAdapter<ImgurImageResponse> jsonAdapter(Moshi moshi) {
        return new AutoValue_ImgurImageResponse.MoshiJsonAdapter(moshi);
    }

}
