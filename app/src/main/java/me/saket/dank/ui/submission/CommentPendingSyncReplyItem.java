package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.PublicContribution;

/**
 * Represents a reply posted by the user that hasn't been synced with the server yet.
 */
@AutoValue
public abstract class CommentPendingSyncReplyItem implements SubmissionCommentRow {

  public abstract PublicContribution parentContribution();

  @Override
  public abstract String fullName();

  public abstract PendingSyncReply pendingSyncReply();

  public abstract boolean isCollapsed();

  public abstract int depth();

  @Override
  public Type type() {
    return Type.PENDING_SYNC_REPLY;
  }

  public static CommentPendingSyncReplyItem create(PublicContribution parentContribution, String fullName, PendingSyncReply pendingSyncReply,
      boolean isCollapsed, int depth)
  {
    return new AutoValue_CommentPendingSyncReplyItem(parentContribution, fullName, pendingSyncReply, isCollapsed, depth);
  }
}
