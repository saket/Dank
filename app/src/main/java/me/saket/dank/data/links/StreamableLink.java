package me.saket.dank.data.links;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class StreamableLink extends MediaLink implements Parcelable {

  @Override
  public abstract String unparsedUrl();

  @Override
  public Link.Type type() {
    return Link.Type.SINGLE_VIDEO;
  }

  @Override
  public abstract String highQualityUrl();

  @Override
  public abstract String lowQualityUrl();

  public static StreamableLink create(String unparsedUrl, String highQualityVideoUrl, String lowQualityVideoUrl) {
    return new AutoValue_StreamableLink(unparsedUrl, highQualityVideoUrl, lowQualityVideoUrl);
  }

  public static JsonAdapter<StreamableLink> jsonAdapter(Moshi moshi) {
    return new AutoValue_StreamableLink.MoshiJsonAdapter(moshi);
  }
}
