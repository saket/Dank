package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import me.saket.dank.data.DankRedditClient;

@AutoValue
public abstract class FocusedComment {

  public abstract String fullname();

  public static FocusedComment create(String id) {
    if (id.startsWith(DankRedditClient.COMMENT_FULLNAME_PREFIX)) {
      throw new AssertionError();
    }
    return new AutoValue_FocusedComment(DankRedditClient.COMMENT_FULLNAME_PREFIX + id);
  }
}
