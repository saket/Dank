package me.saket.dank.data.links;

/**
 * An image, GIF or a video. See implementations.
 */
public abstract class MediaLink extends Link {

  public abstract String highQualityUrl();

  public abstract String lowQualityUrl();
}
