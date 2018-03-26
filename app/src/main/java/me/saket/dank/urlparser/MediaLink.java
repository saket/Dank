package me.saket.dank.urlparser;

/**
 * An image, GIF or a video. See implementations.
 */
public abstract class MediaLink extends Link {

  public abstract String highQualityUrl();

  public abstract String lowQualityUrl();

  public boolean isGif() {
    return UrlParser.isGifUrl(unparsedUrl());
  }
}
