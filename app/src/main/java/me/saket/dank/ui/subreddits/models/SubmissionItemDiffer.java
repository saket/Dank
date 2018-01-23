package me.saket.dank.ui.subreddits.models;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import me.saket.dank.ui.subreddits.SimpleDiffUtilsCallbacks;
import me.saket.dank.ui.subreddits.models.SubredditScreenUiModel.SubmissionRowUiModel;

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
    return oldItem.equals(newItem);
  }

  @Nullable
  @Override
  public Object getChangePayload(SubmissionRowUiModel oldItem, SubmissionRowUiModel newItem) {
    switch (oldItem.type()) {
      case SUBMISSION:
        SubredditSubmission.UiModel oldSubmission = (SubredditSubmission.UiModel) oldItem;
        SubredditSubmission.UiModel newSubmission = (SubredditSubmission.UiModel) newItem;

        List<SubredditSubmission.PartialChange> partialChanges = new ArrayList<>(2);
        if (!oldSubmission.title().equals(newSubmission.title())) {
          partialChanges.add(SubredditSubmission.PartialChange.TITLE);
        }
        if (!oldSubmission.byline().equals(newSubmission.byline())) {
          partialChanges.add(SubredditSubmission.PartialChange.BYLINE);
        }
        if (oldSubmission.thumbnail().isPresent() != newSubmission.thumbnail().isPresent()) {
          throw new AssertionError();
        }
        return partialChanges;

      case PAGINATION_FOOTER:
        return super.getChangePayload(oldItem, newItem);

      default:
        throw new AssertionError();
    }
  }
}
