package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.CommentNode;

/**
 * An item in comment list that can load more comments for its parent.
 */
@AutoValue
public abstract class LoadMoreCommentItem implements SubmissionCommentRow {

  /**
   * The comment node for which more comments can be fetched.
   */
  public abstract CommentNode parentCommentNode();

  /**
   * True when an API call is ongoing to load more comments. False otherwise.
   */
  public abstract boolean progressVisible();

  @Override
  public abstract long id();

  @Override
  public Type type() {
    return Type.LOAD_MORE_COMMENTS;
  }

  public static LoadMoreCommentItem create(CommentNode parentNode, boolean progressVisible) {
    long id = (parentNode.getComment().getId() + "_loadMore").hashCode();
    return new AutoValue_LoadMoreCommentItem(parentNode, progressVisible, id);
  }
}
