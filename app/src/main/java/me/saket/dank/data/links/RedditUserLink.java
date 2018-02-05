package me.saket.dank.data.links;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class RedditUserLink extends RedditLink implements Parcelable {

  @Override
  public abstract String unparsedUrl();

  public abstract String name();

  @Override
  public RedditLinkType redditLinkType() {
    return RedditLinkType.USER;
  }

  public static RedditUserLink create(String unparsedUrl, String userName) {
    return new AutoValue_RedditUserLink(unparsedUrl, userName);
  }
}
