package me.saket.dank.ui.media;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

import me.saket.dank.di.DankApi;

/**
 * API response body of {@link DankApi#imgurAlbum(String)}.
 */
@AutoValue
public abstract class ImgurAlbumResponse implements ImgurResponse {

  @Json(name = "data")
  abstract Data data();

  @Override
  public String id() {
    return data().id();
  }

  @Override
  public boolean hasImages() {
    return data().imageCount() > 0;
  }

  @Override
  public boolean isAlbum() {
    return data().imageCount() > 1;
  }

  @Override
  @Json(name = "images")
  public List<ImgurImage> images() {
    return data().images();
  }

  @Override
  public String albumTitle() {
    return data().albumTitle();
  }

  @Memoized
  @Override
  public String albumCoverImageUrl() {
    String coverImageId = data().coverImageId();
    if (coverImageId != null) {
      return "https://i.imgur.com/" + coverImageId + ".jpg";
    } else {
      return images().get(0).url();
    }
  }

  public static ImgurAlbumResponse createEmpty() {
    return new AutoValue_ImgurAlbumResponse(new Data() {
      @Override
      public String id() {
        return "-1";
      }

      @Override
      int imageCount() {
        return 0;
      }

      @Override
      public List<ImgurImage> images() {
        throw new UnsupportedOperationException("No images here.");
      }

      @Override
      public String albumTitle() {
        return "";
      }

      @Nullable
      @Override
      public String coverImageId() {
        return null;
      }
    });
  }

  @AutoValue
  public abstract static class Data {
    // Fields that aren't present in the free AJAX API are marked as nullable.
    @Json(name = "id")
    public abstract String id();

    @Json(name = "images_count")
    abstract int imageCount();

    @Json(name = "images")
    public abstract List<ImgurImage> images();

    @Nullable
    @Json(name = "title")
    public abstract String albumTitle();

    @Nullable
    @Json(name = "cover")
    public abstract String coverImageId();

    public static JsonAdapter<Data> jsonAdapter(Moshi moshi) {
      return new AutoValue_ImgurAlbumResponse_Data.MoshiJsonAdapter(moshi);
    }
  }

  public static JsonAdapter<ImgurAlbumResponse> jsonAdapter(Moshi moshi) {
    return new AutoValue_ImgurAlbumResponse.MoshiJsonAdapter(moshi);
  }
}
