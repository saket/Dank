package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import me.saket.dank.data.LocallyPostedComment;

/**
 * Represents a reply posted by the user that hasn't been synced with the server yet.
 */
@Deprecated
@AutoValue
public abstract class DankLocallyPostedCommentItem implements SubmissionCommentRow {

  public abstract LocallyPostedComment comment();

  public abstract boolean isCollapsed();

  public abstract int depth();

  @Override
  public String adapterId() {
    return "-1";
  }

  @Override
  public Type type() {
    return Type.PENDING_SYNC_REPLY;
  }

  public static DankLocallyPostedCommentItem create(LocallyPostedComment comment, boolean isCollapsed, int depth) {
    return new AutoValue_DankLocallyPostedCommentItem(comment, isCollapsed, depth);
  }
}
