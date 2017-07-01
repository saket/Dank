package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.CommentNode;

/**
 * Represents a reply posted by the user that hasn't been synced with the server yet.
 */
@AutoValue
public abstract class CommentPendingSyncReplyItem implements SubmissionCommentRow {

  public abstract CommentNode parentCommentNode();

  @Override
  public abstract String fullName();

  public abstract PendingSyncReply pendingSyncReply();

  public abstract boolean isCollapsed();

  @Override
  public Type type() {
    return Type.PENDING_SYNC_REPLY;
  }

  public static CommentPendingSyncReplyItem create(CommentNode parentCommentNode, String fullName, PendingSyncReply pendingSyncReply,
      boolean isCollapsed)
  {
    return new AutoValue_CommentPendingSyncReplyItem(parentCommentNode, fullName, pendingSyncReply, isCollapsed);
  }
}
