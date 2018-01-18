package me.saket.dank.ui.subreddits.models;

import java.util.List;

import me.saket.dank.ui.subreddits.SimpleDiffUtilsCallbacks;
import me.saket.dank.ui.subreddits.models.SubredditScreenUiModel.SubmissionRowUiModel;

public class SubmissionDiffCallbacks extends SimpleDiffUtilsCallbacks<SubmissionRowUiModel> {

  public static SubmissionDiffCallbacks create(List<SubmissionRowUiModel> oldItems, List<SubmissionRowUiModel> newItems) {
    return new SubmissionDiffCallbacks(oldItems, newItems);
  }

  private SubmissionDiffCallbacks(List<SubmissionRowUiModel> oldItems, List<SubmissionRowUiModel> newItems) {
    super(oldItems, newItems);
  }

  @Override
  public boolean areItemsTheSame(SubmissionRowUiModel oldItem, SubmissionRowUiModel newItem) {
    return oldItem.adapterId() == newItem.adapterId();
  }

  @Override
  protected boolean areContentsTheSame(SubmissionRowUiModel oldItem, SubmissionRowUiModel newItem) {
    boolean equals = oldItem.equals(newItem);
//    if (!equals) {
//      Timber.i("Different items:");
//      Timber.i(oldItem.toString());
//      Timber.i(newItem.toString());
//    }
    return equals;
  }
}
