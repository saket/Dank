package me.saket.dank.ui.submission;

import java.util.List;

import me.saket.dank.ui.submission.adapter.SubmissionScreenUiModel;
import me.saket.dank.ui.subreddits.SimpleDiffUtilsCallbacks;

public class CommentsDiffCallback extends SimpleDiffUtilsCallbacks<SubmissionScreenUiModel> {

  public static CommentsDiffCallback create(List<SubmissionScreenUiModel> oldComments, List<SubmissionScreenUiModel> newComments) {
    return new CommentsDiffCallback(oldComments, newComments);
  }

  private CommentsDiffCallback(List<SubmissionScreenUiModel> oldComments, List<SubmissionScreenUiModel> newComments) {
    super(oldComments, newComments);
  }

  /**
   * Called by the DiffUtil to decide whether two object represent the same Item.
   * <p>
   * For example, if your items have unique ids, this method should check their id equality.
   */
  @Override
  public boolean areItemsTheSame(SubmissionScreenUiModel oldCommentRow, SubmissionScreenUiModel newCommentRow) {
    return oldCommentRow.adapterId() == newCommentRow.adapterId();
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
  protected boolean areContentsTheSame(SubmissionScreenUiModel oldCommentRow, SubmissionScreenUiModel newCommentRow) {
    return oldCommentRow.equals(newCommentRow);
  }
}
