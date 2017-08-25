package me.saket.dank.data.links;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class RedditCommentLink extends RedditLink implements Parcelable {

  @Override
  public abstract String unparsedUrl();

  public abstract String id();

  /**
   * Number of parent comments to show.
   */
  public abstract int contextCount();

  public static RedditCommentLink create(String unparsedUrl, String id, Integer contextCount) {
    return new AutoValue_RedditCommentLink(unparsedUrl, id, contextCount);
  }
}
