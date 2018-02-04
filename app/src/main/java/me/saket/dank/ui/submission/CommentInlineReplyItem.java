package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.PublicContribution;

/**
 * Represents an inline field for composing a comment reply.
 */
@Deprecated
@AutoValue
public abstract class CommentInlineReplyItem implements SubmissionCommentRow {

  @Override
  public abstract String adapterId();

  public abstract PublicContribution parentContribution();

  public abstract int depth();

  @Override
  public Type type() {
    return Type.INLINE_REPLY;
  }

  public static CommentInlineReplyItem create(PublicContribution parentContribution, int depth) {
    String fullName = parentContribution.getFullName() + "_reply";
    return new AutoValue_CommentInlineReplyItem(fullName, parentContribution, depth);
  }
}
