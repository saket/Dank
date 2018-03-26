package me.saket.dank.ui.media;

import com.google.auto.value.AutoValue;

import me.saket.dank.urlparser.MediaLink;

@AutoValue
public abstract class MediaLinkWithStartingPosition extends MediaLink {

  abstract MediaLink delegate();

  abstract long startingPositionMillis();

  @Override
  public String highQualityUrl() {
    return delegate().highQualityUrl();
  }

  @Override
  public String lowQualityUrl() {
    return delegate().lowQualityUrl();
  }

  @Override
  public String unparsedUrl() {
    return delegate().unparsedUrl();
  }

  @Override
  public Type type() {
    return delegate().type();
  }

  public static MediaLinkWithStartingPosition create(MediaLink delegate, long startingPositionMillis) {
    return new AutoValue_MediaLinkWithStartingPosition(delegate, startingPositionMillis);
  }
}
