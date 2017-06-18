package me.saket.dank.ui.submission;

import android.support.v7.util.DiffUtil;

import java.util.List;

public class CommentsDiffCallback extends DiffUtil.Callback {

  private final List<SubmissionCommentRow> oldComments;
  private final List<SubmissionCommentRow> newComments;

  public CommentsDiffCallback(List<SubmissionCommentRow> oldComments, List<SubmissionCommentRow> newComments) {
    this.oldComments = oldComments;
    this.newComments = newComments;
  }

  @Override
  public int getOldListSize() {
    return oldComments.size();
  }

  @Override
  public int getNewListSize() {
    return newComments.size();
  }

  /**
   * Called by the DiffUtil to decide whether two object represent the same Item.
   * <p>
   * For example, if your items have unique ids, this method should check their id equality.
   */
  @Override
  public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
    SubmissionCommentRow oldCommentRow = oldComments.get(oldItemPosition);
    SubmissionCommentRow newCommentRow = newComments.get(newItemPosition);
    return oldCommentRow.id() == newCommentRow.id();
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
  public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
    SubmissionCommentRow oldCommentRow = oldComments.get(oldItemPosition);
    SubmissionCommentRow newCommentRow = newComments.get(newItemPosition);
    return oldCommentRow.equals(newCommentRow);
  }
}
