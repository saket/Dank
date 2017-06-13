package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.CommentNode;

@AutoValue
public abstract class DankCommentNode implements SubmissionCommentsRow {

  public abstract CommentNode commentNode();

  public abstract boolean isCollapsed();

  @Override
  public abstract long id();

  @Override
  public SubmissionCommentsRow.Type type() {
    return Type.USER_COMMENT;
  }

  public static DankCommentNode create(CommentNode commentNode, boolean isCollapsed) {
    int commentId = commentNode.getComment().getId().hashCode();
    return new AutoValue_DankCommentNode(commentNode, isCollapsed, commentId);
  }
}
