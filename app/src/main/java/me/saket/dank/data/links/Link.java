package me.saket.dank.data.links;

import android.os.Parcelable;

/**
 * See implementations.
 */
public abstract class Link implements Parcelable {

  public enum Type {
    /**
     * Submission / user / subreddit.
     */
    REDDIT_PAGE,

    SINGLE_IMAGE_OR_GIF,

    SINGLE_VIDEO,

    MEDIA_ALBUM,

    /**
     * A link that will be opened in a browser.
     */
    EXTERNAL,
  }

  public abstract String unparsedUrl();

  public abstract Type type();

  public boolean isImageOrGif() {
    return type() == Type.SINGLE_IMAGE_OR_GIF;
  }

  public boolean isVideo() {
    return type() == Type.SINGLE_VIDEO;
  }

  public boolean isExternal() {
    return type() == Type.EXTERNAL;
  }

  public boolean isRedditPage() {
    return type() == Type.REDDIT_PAGE;
  }

  public boolean isMediaAlbum() {
    return type() == Type.MEDIA_ALBUM;
  }
}
