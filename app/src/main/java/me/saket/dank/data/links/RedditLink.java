package me.saket.dank.data.links;

/**
 * A reddit.com link.
 */
public abstract class RedditLink extends Link {

  public enum RedditLinkType {
    COMMENT,
    SUBMISSION,
    SUBREDDIT,
    USER,
  }

  @Override
  public Link.Type type() {
    return Link.Type.REDDIT_PAGE;
  }

  public abstract RedditLinkType redditLinkType();
}
