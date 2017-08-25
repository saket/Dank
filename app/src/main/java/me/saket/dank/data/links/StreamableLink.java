package me.saket.dank.data.links;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

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

  public static StreamableLink create(String unparsedUrl, String lowQualityVideoUrl, String highQualityVideoUrl) {
    return new AutoValue_StreamableLink(unparsedUrl, lowQualityVideoUrl, highQualityVideoUrl);
  }
}
