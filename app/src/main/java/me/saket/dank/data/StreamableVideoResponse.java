package me.saket.dank.data;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import me.saket.dank.di.DankApi;
import me.saket.dank.utils.Optional;

/**
 * API response of {@link DankApi#streamableVideoDetails(String)}.
 */
@AutoValue
public abstract class StreamableVideoResponse {

  @Json(name = "files")
  public abstract Files files();

  /**
   * Note: URL doesn't contain the scheme or '://'.
   * E.g.: 'streamable.com/fxn88'. Not sure if 'protocol' is the right word.
   */
  @Json(name = "url")
  abstract String urlWithoutProtocol();

  public String url() {
    return "https://" + urlWithoutProtocol();
  }

  @AutoValue
  public abstract static class Files {
    @Json(name = "mp4")
    public abstract Video highQualityVideo();

    @Json(name = "mp4-mobile")
    public abstract Optional<Video> lowQualityVideo();

    public static JsonAdapter<Files> jsonAdapter(Moshi moshi) {
      return new AutoValue_StreamableVideoResponse_Files.MoshiJsonAdapter(moshi);
    }
  }

  @AutoValue
  public abstract static class Video {
    /**
     * Note: the URL is scheme-less.
     * E.g.: '//cdn-e2.streamable.com/video/mp4-mobile/fxn88.mp4?token=1492269114_8becb8f6fab5b53e5b0ee0f0c6a8f8969e38a04a'.
     */
    @Json(name = "url")
    abstract String schemeLessUrl();

    public Optional<String> url() {
      return schemeLessUrl().isEmpty()
          ? Optional.empty()
          : Optional.of("https:" + schemeLessUrl());
    }

    public static JsonAdapter<Video> jsonAdapter(Moshi moshi) {
      return new AutoValue_StreamableVideoResponse_Video.MoshiJsonAdapter(moshi);
    }
  }

  public static JsonAdapter<StreamableVideoResponse> jsonAdapter(Moshi moshi) {
    return new AutoValue_StreamableVideoResponse.MoshiJsonAdapter(moshi);
  }

}
