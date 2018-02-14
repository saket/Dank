package me.saket.dank.data.links;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class RedditHostedVideoLink extends RedditLink implements Parcelable {

  @Override
  public RedditLinkType redditLinkType() {
    return RedditLinkType.VIDEO;
  }

  @Override
  public abstract String unparsedUrl();

  public static RedditHostedVideoLink create(String unparsedUrl) {
    return new AutoValue_RedditHostedVideoLink(unparsedUrl);
  }
}
