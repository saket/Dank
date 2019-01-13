package me.saket.dank.ui.subreddit.uimodels;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import me.saket.dank.utils.SimpleDiffUtilsCallbacks;
import me.saket.dank.ui.subreddit.uimodels.SubredditScreenUiModel.SubmissionRowUiModel;

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

        List<SubredditSubmission.PartialChange> partialChanges = new ArrayList<>(3);
        if (oldSubmission.displayThumbnailOnLeftSide() != newSubmission.displayThumbnailOnLeftSide()) {
          partialChanges.add(SubredditSubmission.PartialChange.THUMBNAIL_POSITION);
        }
        if (!oldSubmission.title().equals(newSubmission.title())) {
          partialChanges.add(SubredditSubmission.PartialChange.TITLE);
        }
        if (!oldSubmission.byline().equals(newSubmission.byline())) {
          partialChanges.add(SubredditSubmission.PartialChange.BYLINE);
        }
        if (!oldSubmission.thumbnail().equals(newSubmission.thumbnail())) {
          partialChanges.add(SubredditSubmission.PartialChange.THUMBNAIL);
        }
        if (oldSubmission.isSaved() != newSubmission.isSaved()) {
          partialChanges.add(SubredditSubmission.PartialChange.SAVE_STATUS);
        }
        return partialChanges;

      case PAGINATION_FOOTER:
      case GESTURES_WALKTHROUGH:
        return super.getChangePayload(oldItem, newItem);

      default:
        throw new AssertionError();
    }
  }
}
