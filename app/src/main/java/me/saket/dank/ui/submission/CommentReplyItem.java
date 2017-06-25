package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.CommentNode;

/**
 * Used in comment list.
 */
@AutoValue
public abstract class CommentReplyItem implements SubmissionCommentRow{

  public abstract long id();

  public abstract CommentNode commentNodeToReply();

  @Override
  public Type type() {
    return Type.REPLY;
  }

  public static CommentReplyItem create(CommentNode nodeToReply) {
    long id = (nodeToReply.getComment().getId() + "_reply").hashCode();
    return new AutoValue_CommentReplyItem(id, nodeToReply);
  }
}
