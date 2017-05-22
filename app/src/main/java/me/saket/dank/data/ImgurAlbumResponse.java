package me.saket.dank.data;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

import me.saket.dank.di.DankApi;

/**
 * API response body of {@link DankApi#imgurAlbumPaid(String)}.
 */
@AutoValue
public abstract class ImgurAlbumResponse implements ImgurResponse {

  @Json(name = "data")
  abstract Data data();

  @Override
  public boolean hasImages() {
    return data().imageCount() > 0;
  }

  @Override
  public boolean isAlbum() {
    return data().imageCount() > 1;
  }

  @Override
  public List<ImgurImage> images() {
    return data().images();
  }

  @Override
  public String albumTitle() {
    return data().albumTitle();
  }

  public static ImgurAlbumResponse createEmpty() {
    return new AutoValue_ImgurAlbumResponse(new Data() {
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
        return null;
      }
    });
  }

  @AutoValue
  public abstract static class Data {
    @Json(name = "images_count")
    abstract int imageCount();

    @Json(name = "images")
    public abstract List<ImgurImage> images();

    @Nullable
    @Json(name = "title")
    public abstract String albumTitle();

    public static JsonAdapter<Data> jsonAdapter(Moshi moshi) {
      return new AutoValue_ImgurAlbumResponse_Data.MoshiJsonAdapter(moshi);
    }
  }

  public static JsonAdapter<ImgurAlbumResponse> jsonAdapter(Moshi moshi) {
    return new AutoValue_ImgurAlbumResponse.MoshiJsonAdapter(moshi);
  }

}
