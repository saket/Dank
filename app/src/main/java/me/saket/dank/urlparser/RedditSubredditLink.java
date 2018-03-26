package me.saket.dank.urlparser;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class RedditSubredditLink extends RedditLink implements Parcelable {

  @Override
  public abstract String unparsedUrl();

  public abstract String name();

  @Override
  public RedditLinkType redditLinkType() {
    return RedditLinkType.SUBREDDIT;
  }

  public static RedditSubredditLink create(String unparsedUrl, String subredditName) {
    return new AutoValue_RedditSubredditLink(unparsedUrl, subredditName);
  }
}
