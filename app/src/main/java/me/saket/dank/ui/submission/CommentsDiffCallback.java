package me.saket.dank.ui.submission;

import java.util.List;

import me.saket.dank.ui.subreddits.SimpleDiffUtilsCallbacks;
import timber.log.Timber;

public class CommentsDiffCallback extends SimpleDiffUtilsCallbacks<SubmissionCommentRow> {

  public CommentsDiffCallback(List<SubmissionCommentRow> oldComments, List<SubmissionCommentRow> newComments) {
    super(oldComments, newComments);
  }

  /**
   * Called by the DiffUtil to decide whether two object represent the same Item.
   * <p>
   * For example, if your items have unique ids, this method should check their id equality.
   */
  @Override
  public boolean areItemsTheSame(SubmissionCommentRow oldCommentRow, SubmissionCommentRow newCommentRow) {
    return oldCommentRow.fullName().equals(newCommentRow.fullName());
  }

  /**
   * Called by the DiffUtil when it wants to check whether two items have the same data.
   * DiffUtil uses this information to detect if the contents of an item has changed.
   * <p>
   * DiffUtil uses this method to check equality instead of {@link Object#equals(Object)}
   * so that you can change its behavior depending on your UI.
   * For example, if you are using DiffUtil with a
   * {@link android.support.v7.widget.RecyclerView.Adapter RecyclerView.Adapter}, you should
   * return whether the items' visual representations are the same.
   * <p>
   * This method is called only if {@link #areItemsTheSame(int, int)} returns
   * {@code true} for these items.
   */
  @Override
  protected boolean areContentsTheSame(SubmissionCommentRow oldCommentRow, SubmissionCommentRow newCommentRow) {
    try {
      return oldCommentRow.equals(newCommentRow);
    } catch (StackOverflowError e) {
      Timber.e("StackOverflowError while equals");
      return false;
    }
  }
}
