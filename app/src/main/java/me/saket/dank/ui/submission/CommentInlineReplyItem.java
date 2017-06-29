package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.CommentNode;

/**
 * Represents an inline field for composing a comment reply.
 */
@AutoValue
public abstract class CommentInlineReplyItem implements SubmissionCommentRow {

  @Override
  public abstract String fullName();

  public abstract CommentNode parentCommentNode();

  @Override
  public Type type() {
    return Type.REPLY;
  }

  public static CommentInlineReplyItem create(CommentNode parentCommentNode) {
    String fullName = parentCommentNode.getComment().getFullName() + "_reply";
    return new AutoValue_CommentInlineReplyItem(fullName, parentCommentNode);
  }
}
