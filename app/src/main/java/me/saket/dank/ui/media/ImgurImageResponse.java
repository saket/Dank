package me.saket.dank.ui.media;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.Collections;
import java.util.List;

import me.saket.dank.di.DankApi;

/**
 * API response body of {@link DankApi#imgurImage(String)}.
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

  @Memoized
  @Override
  public List<ImgurImage> images() {
    return Collections.singletonList(image());
  }

  @Override
  public String albumTitle() {
    // Not valid.
    throw new UnsupportedOperationException("Image doesn't have any album title");
  }

  @Override
  public String albumCoverImageUrl() {
    return image().url();
  }

  public static JsonAdapter<ImgurImageResponse> jsonAdapter(Moshi moshi) {
    return new AutoValue_ImgurImageResponse.MoshiJsonAdapter(moshi);
  }
}
