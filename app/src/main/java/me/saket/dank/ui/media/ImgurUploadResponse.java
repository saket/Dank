package me.saket.dank.ui.media;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import me.saket.dank.di.DankApi;
import okhttp3.MultipartBody;

/**
 * Response body for {@link DankApi#uploadToImgur(MultipartBody.Part, String)}.
 */
@AutoValue
public abstract class ImgurUploadResponse {

  @Json(name = "data")
  public abstract Data data();

  @Json(name = "success")
  public abstract boolean success();

  @Json(name = "status")
  public abstract int status();

  @AutoValue
  public abstract static class Data {
    @Nullable
    public abstract String error();

    @Nullable
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
