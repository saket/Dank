package me.saket.dank.data;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.File;

import me.saket.dank.di.DankApi;

/**
 * Response body for {@link DankApi#uploadToImgur(File, String)}.
 */
@AutoValue
public abstract class ImgurUploadResponse {

  @Json(name = "data")
  public abstract Data data();

  @Json(name = "success")
  public abstract boolean success();

  @AutoValue
  public abstract static class Data {
    @Json(name = "link")
    public abstract String link();

    public static JsonAdapter<ImgurUploadResponse.Data> jsonAdapter(Moshi moshi) {
      return new AutoValue_ImgurUploadResponse_Data.MoshiJsonAdapter(moshi);
    }
  }

  public static JsonAdapter<ImgurUploadResponse> jsonAdapter(Moshi moshi) {
    return new AutoValue_ImgurUploadResponse.MoshiJsonAdapter(moshi);
  }
}
