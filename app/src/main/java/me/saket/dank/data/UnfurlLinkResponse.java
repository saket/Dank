package me.saket.dank.data;

import androidx.annotation.Nullable;
import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import me.saket.dank.urlparser.LinkMetadata;
import me.saket.dank.di.DankApi;

/**
 * Api response for {@link DankApi#unfurlUrl(String, boolean)}.
 */
@AutoValue
public abstract class UnfurlLinkResponse {

  @Nullable
  @Json(name = "data")
  public abstract Data data();

  @Nullable
  @Json(name = "errors")
  public abstract Error error();

  public static JsonAdapter<UnfurlLinkResponse> jsonAdapter(Moshi moshi) {
    return new AutoValue_UnfurlLinkResponse.MoshiJsonAdapter(moshi);
  }

  @AutoValue
  public abstract static class Data {

    @Json(name = "type")
    public abstract String type();

    @Json(name = "attributes")
    public abstract LinkMetadata linkMetadata();

    public static JsonAdapter<Data> jsonAdapter(Moshi moshi) {
      return new AutoValue_UnfurlLinkResponse_Data.MoshiJsonAdapter(moshi);
    }
  }

  @AutoValue
  public abstract static class Error {
    @Json(name = "status")
    public abstract int statusCode();

    @Json(name = "message")
    public abstract String message();

    public static JsonAdapter<Error> jsonAdapter(Moshi moshi) {
      return new AutoValue_UnfurlLinkResponse_Error.MoshiJsonAdapter(moshi);
    }
  }
}
