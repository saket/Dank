package me.saket.dank.ui.submission.adapter;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import me.saket.dank.ui.subreddits.SimpleDiffUtilsCallbacks;
import me.saket.dank.utils.Optional;

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
    if (oldItem.getClass() != newItem.getClass()) {
      throw new AssertionError();
    }

    switch (oldItem.type()) {
      case SUBMISSION_HEADER: {
        List<SubmissionCommentsHeader.PartialChange> partialChanges = new ArrayList<>(4);
        SubmissionCommentsHeader.UiModel oldHeader = (SubmissionCommentsHeader.UiModel) oldItem;
        SubmissionCommentsHeader.UiModel newHeader = (SubmissionCommentsHeader.UiModel) newItem;

        Optional<SubmissionContentLinkUiModel> oldContentLink = oldHeader.optionalContentLinkModel();
        Optional<SubmissionContentLinkUiModel> newContentLink = newHeader.optionalContentLinkModel();

        //Timber.i("--------------------------");
        //Timber.i("oldContentLink: %s", oldContentLink);
        //Timber.i("newContentLink: %s", newContentLink);

        if (oldContentLink.isPresent() != newContentLink.isPresent()) {
          partialChanges.add(SubmissionCommentsHeader.PartialChange.CONTENT_LINK);

        } else if (oldContentLink.isPresent() && newContentLink.isPresent()) {
          if (!oldContentLink.get().thumbnail().isPresent() && newContentLink.get().thumbnail().isPresent()) {
            partialChanges.add(SubmissionCommentsHeader.PartialChange.CONTENT_LINK_THUMBNAIL);
          }
          if (!oldContentLink.get().icon().isPresent() && newContentLink.get().icon().isPresent()) {
            partialChanges.add(SubmissionCommentsHeader.PartialChange.CONTENT_LINK_FAVICON);
          }
          if (oldContentLink.get().progressVisible() != newContentLink.get().progressVisible()) {
            partialChanges.add(SubmissionCommentsHeader.PartialChange.CONTENT_LINK_PROGRESS_VISIBILITY);
          }
          if (!oldContentLink.get().title().equals(newContentLink.get().title())
              || !oldContentLink.get().byline().equals(newContentLink.get().byline())
              || oldContentLink.get().titleMaxLines() != newContentLink.get().titleMaxLines()
              || oldContentLink.get().titleTextColorRes() != newContentLink.get().titleTextColorRes()
              || oldContentLink.get().bylineTextColorRes() != newContentLink.get().bylineTextColorRes())
          {
            partialChanges.add(SubmissionCommentsHeader.PartialChange.CONTENT_LINK_TITLE_AND_BYLINE);
          }
          if (!oldContentLink.get().backgroundTintColor().equals(newContentLink.get().backgroundTintColor())) {
            partialChanges.add(SubmissionCommentsHeader.PartialChange.CONTENT_LINK_TINT);
          }
        }

        if (oldHeader.extraInfoForEquality().votes() != newHeader.extraInfoForEquality().votes()) {
          partialChanges.add(SubmissionCommentsHeader.PartialChange.SUBMISSION_VOTE);
        }
        if (oldHeader.extraInfoForEquality().commentsCount().equals(newHeader.extraInfoForEquality().commentsCount())) {
          partialChanges.add(SubmissionCommentsHeader.PartialChange.SUBMISSION_COMMENT_COUNT);
        }
        return partialChanges;
      }

      case USER_COMMENT: {
        List<SubmissionComment.PartialChange> partialChanges = new ArrayList<>(2);
        SubmissionComment.UiModel oldComment = (SubmissionComment.UiModel) oldItem;
        SubmissionComment.UiModel newComment = (SubmissionComment.UiModel) newItem;

        if (oldComment.isCollapsed() == newComment.isCollapsed() && (!oldComment.byline().equals(newComment.byline())
            || oldComment.extraInfoForEquality().voteScore() != newComment.extraInfoForEquality().voteScore()))
        {
          partialChanges.add(SubmissionComment.PartialChange.BYLINE);
          return partialChanges;
        } else {
          // In case of comment collapse, a full re-bind is required AND the
          // change animation also needs to be played to show change in body size.
          return null;
        }
      }

      default:
        return null;
    }
  }
}
