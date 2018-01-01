package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.CommentNode;

/**
 * Represents an item that can load more comments for its parent.
 */
@AutoValue
@Deprecated
public abstract class LoadMoreCommentItem implements SubmissionCommentRow {

  @Override
  public abstract String fullName();

  /**
   * The comment node for which more comments can be fetched.
   */
  public abstract CommentNode parentCommentNode();

  /**
   * True when an API call is ongoing to load more comments. False otherwise.
   */
  public abstract boolean progressVisible();

  @Override
  public Type type() {
    return Type.LOAD_MORE_COMMENTS;
  }

  public static LoadMoreCommentItem create(String parentNodeFullname, CommentNode parentNode, boolean progressVisible) {
    if (parentNodeFullname == null) {
      throw new NullPointerException();
    }
    String fullName = parentNodeFullname + "_loadMore";
    return new AutoValue_LoadMoreCommentItem(fullName, parentNode, progressVisible);
  }
}
