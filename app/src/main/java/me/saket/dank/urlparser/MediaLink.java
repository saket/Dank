package me.saket.dank.urlparser;

/**
 * An image, GIF or a video. See implementations.
 */
public abstract class MediaLink extends Link {

  public abstract String highQualityUrl();

  public abstract String lowQualityUrl();

  public abstract String cacheKey();

  public boolean isGif() {
    return UrlParser.isGifUrl(unparsedUrl());
  }

  public String cacheKeyWithClassName(String key) {
    String name = getClass().getSimpleName();
    String nameWithoutAutoValue = name.startsWith("AutoValue_")
        ? name.substring("AutoValue_".length())
        : name;
    return nameWithoutAutoValue + "_" + key;
  }
}
