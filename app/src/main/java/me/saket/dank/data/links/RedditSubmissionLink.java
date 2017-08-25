package me.saket.dank.data.links;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class RedditSubmissionLink extends RedditLink implements Parcelable {

  @Override
  public abstract String unparsedUrl();

  public abstract String id();

  @Nullable
  public abstract String subredditName();

  /**
   * This is non-null if a comment's permalink was clicked. This comment is shown as
   * the root comment of the submission.
   */
  @Nullable public abstract RedditCommentLink initialComment();

  public static RedditSubmissionLink create(String unparsedUrl, String id, String subredditName) {
    return new AutoValue_RedditSubmissionLink(unparsedUrl, id, subredditName, null);
  }

  public static RedditSubmissionLink createWithComment(String unparsedUrl, String id, String subredditName, RedditCommentLink initialComment) {
    return new AutoValue_RedditSubmissionLink(unparsedUrl, id, subredditName, initialComment);
  }
}
