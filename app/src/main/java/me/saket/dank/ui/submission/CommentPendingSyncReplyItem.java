package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.CommentNode;

/**
 * Represents a reply posted by the user that hasn't been synced with the server yet.
 */
@AutoValue
public abstract class CommentPendingSyncReplyItem implements SubmissionCommentRow {

  public abstract long id();

  public abstract PendingSyncReply pendingSyncReply();

  public abstract int parentCommentNodeDepth();

  public abstract boolean isCollapsed();

  @Override
  public Type type() {
    return Type.PENDING_SYNC_REPLY;
  }

  public static CommentPendingSyncReplyItem create(CommentNode parentCommentNode, PendingSyncReply pendingSyncReply, int depth, boolean isCollapsed) {
    long id = (parentCommentNode.getComment().getId() + "_reply_ " + pendingSyncReply.createdTimeMillis()).hashCode();
    return new AutoValue_CommentPendingSyncReplyItem(id, pendingSyncReply, depth, isCollapsed);
  }
}
