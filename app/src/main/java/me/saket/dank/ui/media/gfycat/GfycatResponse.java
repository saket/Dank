package me.saket.dank.ui.media.gfycat;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class GfycatResponse {

  @Json(name = "gfyItem")
  public abstract Data data();

  public static JsonAdapter<GfycatResponse> jsonAdapter(Moshi moshi) {
    return new AutoValue_GfycatResponse.MoshiJsonAdapter(moshi);
  }

  public static GfycatResponse create(Data data) {
    return new AutoValue_GfycatResponse(data);
  }

  @AutoValue
  public abstract static class Data {

    @Json(name = "gfyName")
    public abstract String threeWordId();

    @Json(name = "webmUrl")
    public abstract String highQualityUrl();

    @Json(name = "miniUrl")
    public abstract String lowQualityUrl();

    public static JsonAdapter<Data> jsonAdapter(Moshi moshi) {
      return new AutoValue_GfycatResponse_Data.MoshiJsonAdapter(moshi);
    }

    public static Data create(String threeWordId, String highQualityUrl, String lowQualityUrl) {
      return new AutoValue_GfycatResponse_Data(threeWordId, highQualityUrl, lowQualityUrl);
    }
  }
}
