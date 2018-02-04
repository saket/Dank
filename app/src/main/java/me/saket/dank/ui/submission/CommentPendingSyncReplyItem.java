package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.PublicContribution;

/**
 * Represents a reply posted by the user that hasn't been synced with the server yet.
 */
@Deprecated
@AutoValue
public abstract class CommentPendingSyncReplyItem implements SubmissionCommentRow {

  public abstract PublicContribution parentContribution();

  @Override
  public abstract String adapterId();

  public abstract PendingSyncReply pendingSyncReply();

  public abstract boolean isCollapsed();

  public abstract int depth();

  @Override
  public Type type() {
    return Type.PENDING_SYNC_REPLY;
  }

  public static CommentPendingSyncReplyItem create(
      PublicContribution parentContribution,
      String adapterId,
      PendingSyncReply pendingSyncReply,
      boolean isCollapsed,
      int depth)
  {
    return new AutoValue_CommentPendingSyncReplyItem(parentContribution, adapterId, pendingSyncReply, isCollapsed, depth);
  }
}
