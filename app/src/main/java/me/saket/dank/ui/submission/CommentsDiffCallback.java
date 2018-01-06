package me.saket.dank.ui.submission;

import android.os.Bundle;
import android.support.annotation.Nullable;

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

  @Override
  public boolean areItemsTheSame(SubmissionScreenUiModel oldModel, SubmissionScreenUiModel newModel) {
    return oldModel.adapterId() == newModel.adapterId();
  }

  @Override
  protected boolean areContentsTheSame(SubmissionScreenUiModel oldModel, SubmissionScreenUiModel newModel) {
    return oldModel.equals(newModel);
  }

  @Nullable
  @Override
  public Object getChangePayload(SubmissionScreenUiModel oldItem, SubmissionScreenUiModel newItem) {
    switch (oldItem.type()) {
      case SUBMISSION_HEADER:
        // TODO: bundle for:
        // 1. Thumbnail in content link.
        // 2. Tint color.
        // 3. Content link details.
        // 4. Comment count.
        // 5. Vote count.
        return new Bundle();

      case USER_COMMENT:
        // TODO:
        // 1. Vote count.
        return null;

      default:
        return super.getChangePayload(oldItem, newItem);
    }
  }
}
