package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.CommentNode;

/**
 * Represents an inline field for composing a comment reply.
 */
@AutoValue
public abstract class CommentInlineReplyItem implements SubmissionCommentRow {

  public abstract long id();

  public abstract CommentNode parentCommentNode();

  @Override
  public Type type() {
    return Type.REPLY;
  }

  public static CommentInlineReplyItem create(CommentNode parentCommentNode) {
    long id = (parentCommentNode.getComment().getId() + "_reply").hashCode();
    return new AutoValue_CommentInlineReplyItem(id, parentCommentNode);
  }
}
