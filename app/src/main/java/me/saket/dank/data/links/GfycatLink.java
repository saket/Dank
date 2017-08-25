package me.saket.dank.data.links;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

/**
 * Gfycat.com for GIFs (converted to MP4).
 */
@AutoValue
public abstract class GfycatLink extends MediaLink implements Parcelable {

  @Override
  public abstract String unparsedUrl();

  @Override
  public abstract String highQualityUrl();

  @Override
  public abstract String lowQualityUrl();

  @Override
  public Link.Type type() {
    return Link.Type.SINGLE_VIDEO;
  }

  public static GfycatLink create(String unparsedUrl, String highQualityVideoUrl, String lowQualityVideoUrl) {
    return new AutoValue_GfycatLink(unparsedUrl, highQualityVideoUrl, lowQualityVideoUrl);
  }
}
