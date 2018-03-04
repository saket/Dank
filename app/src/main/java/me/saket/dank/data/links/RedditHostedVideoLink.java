package me.saket.dank.data.links;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

/**
 * Reddit uses DASH for its videos, which automatically switches between video qualities
 * depending upon the user's network connection, which is fantastic!
 */
@AutoValue
public abstract class RedditHostedVideoLink extends MediaLink implements Parcelable {

  @Override
  public Type type() {
    return Type.SINGLE_VIDEO;
  }

  @Override
  public abstract String unparsedUrl();

  @Override
  public abstract String highQualityUrl();

  @Override
  public abstract String lowQualityUrl();

  public abstract String directUrlWithoutAudio();

  public static RedditHostedVideoLink create(
      String unparsedUrl,
      String highQualityDashPlaylistUrl,
      String lowQualityUrl,
      String directUrlWithoutAudio)
  {
    return new AutoValue_RedditHostedVideoLink(unparsedUrl, highQualityDashPlaylistUrl, lowQualityUrl, directUrlWithoutAudio);
  }
}
