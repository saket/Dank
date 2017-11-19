package me.saket.dank.ui.giphy;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

import me.saket.dank.di.DankApi;

/**
 * API response for {@link DankApi#giphySearch(String, String, int, int)} and {@link DankApi#giphyTrending(String, int, int)}.
 */
@AutoValue
public abstract class GiphySearchResponse {

  @Json(name = "data")
  public abstract List<GiphyItem> items();

  @Json(name = "pagination")
  public abstract PaginationInfo paginationInfo();

  public static JsonAdapter<GiphySearchResponse> jsonAdapter(Moshi moshi) {
    return new AutoValue_GiphySearchResponse.MoshiJsonAdapter(moshi);
  }

  @AutoValue
  public abstract static class GiphyItem {
    @Json(name = "id")
    public abstract String id();

    @Json(name = "title")
    public abstract String title();

    @Json(name = "url")
    public abstract String url();

    @Json(name = "images")
    public abstract GifVariants gifVariants();

    public static JsonAdapter<GiphyItem> jsonAdapter(Moshi moshi) {
      return new AutoValue_GiphySearchResponse_GiphyItem.MoshiJsonAdapter(moshi);
    }
  }

  @AutoValue
  public abstract static class GifVariants {
    /**
     * Data surrounding a version of this GIF downsized to be under 2mb.
     */
    @Json(name = "downsized")
    public abstract GifVariant downsizedUnder2mb();

    /**
     * Data surrounding versions of this GIF with a fixed height of 200 pixels. Good for mobile use.
     */
    @Json(name = "fixed_height")
    public abstract GifVariant fixedHeight200px();

    public static JsonAdapter<GifVariants> jsonAdapter(Moshi moshi) {
      return new AutoValue_GiphySearchResponse_GifVariants.MoshiJsonAdapter(moshi);
    }
  }

  @AutoValue
  public abstract static class GifVariant {
    @Json(name = "url")
    public abstract String url();

    @Json(name = "width")
    public abstract String width();

    @Json(name = "height")
    public abstract String height();

    @Json(name = "size")
    public abstract String sizeBytes();

    public static JsonAdapter<GifVariant> jsonAdapter(Moshi moshi) {
      return new AutoValue_GiphySearchResponse_GifVariant.MoshiJsonAdapter(moshi);
    }
  }

  @AutoValue
  public abstract static class PaginationInfo {
    @Json(name = "offset")
    public abstract int offset();

    @Json(name = "total_count")
    public abstract int totalCount();

    @Json(name = "count")
    public abstract int count();

    public static JsonAdapter<PaginationInfo> jsonAdapter(Moshi moshi) {
      return new AutoValue_GiphySearchResponse_PaginationInfo.MoshiJsonAdapter(moshi);
    }
  }
}
