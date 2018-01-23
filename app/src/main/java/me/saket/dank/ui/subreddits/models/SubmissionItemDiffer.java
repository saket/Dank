package me.saket.dank.ui.subreddits.models;

import java.util.List;

import me.saket.dank.ui.subreddits.SimpleDiffUtilsCallbacks;
import me.saket.dank.ui.subreddits.models.SubredditScreenUiModel.SubmissionRowUiModel;
import timber.log.Timber;

public class SubmissionItemDiffer extends SimpleDiffUtilsCallbacks<SubmissionRowUiModel> {

  public static SubmissionItemDiffer create(List<SubmissionRowUiModel> oldItems, List<SubmissionRowUiModel> newItems) {
    return new SubmissionItemDiffer(oldItems, newItems);
  }

  private SubmissionItemDiffer(List<SubmissionRowUiModel> oldItems, List<SubmissionRowUiModel> newItems) {
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
