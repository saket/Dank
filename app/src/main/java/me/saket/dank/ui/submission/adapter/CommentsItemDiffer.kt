package me.saket.dank.ui.submission.adapter

import me.saket.dank.utils.SimpleDiffUtilsCallbacks
import java.util.ArrayList

class CommentsItemDiffer private constructor(
    oldComments: List<SubmissionScreenUiModel>,
    newComments: List<SubmissionScreenUiModel>
) : SimpleDiffUtilsCallbacks<SubmissionScreenUiModel>(oldComments, newComments) {

  override fun areItemsTheSame(oldModel: SubmissionScreenUiModel, newModel: SubmissionScreenUiModel): Boolean {
    return oldModel.adapterId() == newModel.adapterId()
  }

  override fun areContentsTheSame(oldModel: SubmissionScreenUiModel, newModel: SubmissionScreenUiModel): Boolean {
    return oldModel == newModel
  }

  override fun getChangePayload(oldItem: SubmissionScreenUiModel, newItem: SubmissionScreenUiModel): Any? {
    if (oldItem.javaClass != newItem.javaClass) {
      throw AssertionError()
    }

    when (oldItem.type()) {
      SubmissionCommentRowType.SUBMISSION_HEADER -> {
        val oldHeader = oldItem as SubmissionCommentsHeader.UiModel
        val newHeader = newItem as SubmissionCommentsHeader.UiModel

        val oldContentLink = oldHeader.optionalContentLinkModel()
        val newContentLink = newHeader.optionalContentLinkModel()

        //Timber.i("--------------------------");
        //Timber.i("oldContentLink: %s", oldContentLink);
        //Timber.i("newContentLink: %s", newContentLink);

        val partialChanges = ArrayList<SubmissionCommentsHeader.PartialChange>(8)

        if (oldContentLink.isPresent != newContentLink.isPresent) {
          partialChanges.add(SubmissionCommentsHeader.PartialChange.CONTENT_LINK)

        } else if (oldContentLink.isPresent && newContentLink.isPresent) {
          if (!oldContentLink.get().thumbnail().isPresent && newContentLink.get().thumbnail().isPresent) {
            //Timber.d("Thumb diff");
            partialChanges.add(SubmissionCommentsHeader.PartialChange.CONTENT_LINK_THUMBNAIL)
          }
          if (oldContentLink.get().icon() != newContentLink.get().icon()) {
            //Timber.d("Link favicon diff: %s vs %s", oldContentLink.get().icon(), newContentLink.get().icon());
            partialChanges.add(SubmissionCommentsHeader.PartialChange.CONTENT_LINK_FAVICON)
          }
          if (oldContentLink.get().progressVisible() != newContentLink.get().progressVisible()) {
            //Timber.d("Link progress diff");
            partialChanges.add(SubmissionCommentsHeader.PartialChange.CONTENT_LINK_PROGRESS_VISIBILITY)
          }
          if (oldContentLink.get().title() != newContentLink.get().title()
              || oldContentLink.get().byline() != newContentLink.get().byline()
              || oldContentLink.get().titleMaxLines() != newContentLink.get().titleMaxLines()
              || oldContentLink.get().titleTextColorRes() != newContentLink.get().titleTextColorRes()
              || oldContentLink.get().bylineTextColorRes() != newContentLink.get().bylineTextColorRes()) {
            //Timber.d("Link title/byline is different");
            partialChanges.add(SubmissionCommentsHeader.PartialChange.CONTENT_LINK_TITLE_AND_BYLINE)
          }
          if (oldContentLink.get().backgroundTintColor() != newContentLink.get().backgroundTintColor()) {
            //Timber.d("Tint is different: %s vs %s", oldContentLink.get().backgroundTintColor(), newContentLink.get().backgroundTintColor());
            partialChanges.add(SubmissionCommentsHeader.PartialChange.CONTENT_LINK_TINT)
          }
        }

        if (oldHeader.title() != newHeader.title()) {
          partialChanges.add(SubmissionCommentsHeader.PartialChange.SUBMISSION_TITLE)
        }
        if (oldHeader.byline() != newHeader.byline()) {
          partialChanges.add(SubmissionCommentsHeader.PartialChange.SUBMISSION_BYLINE)
        }
        if (oldHeader.isSaved != newHeader.isSaved) {
          partialChanges.add(SubmissionCommentsHeader.PartialChange.SUBMISSION_SAVE_STATUS)
        }
        if (oldHeader.swipeActions() != newHeader.swipeActions()) {
          partialChanges.add(SubmissionCommentsHeader.PartialChange.SUBMISSION_SWIPE_ACTIONS)
        }

        //Timber.i("--------------------");
        //Timber.i(partialChanges.toString());
        return partialChanges
      }

      SubmissionCommentRowType.REMOTE_USER_COMMENT -> {
        val oldComment = oldItem as SubmissionRemoteComment.UiModel
        val newComment = newItem as SubmissionRemoteComment.UiModel

        return if (oldComment.isCollapsed == newComment.isCollapsed && oldComment.byline() != newComment.byline()) {
          listOf(SubmissionRemoteComment.PartialChange.BYLINE)
        } else {
          // In case of comment collapse, a full re-bind is required AND the
          // change animation also needs to be played to show change in body size.
          null
        }
      }

      SubmissionCommentRowType.LOCAL_USER_COMMENT -> {
        val oldComment = oldItem as SubmissionLocalComment.UiModel
        val newComment = newItem as SubmissionLocalComment.UiModel

        return if (oldComment.isCollapsed == newComment.isCollapsed && oldComment.byline != newComment.byline) {
          listOf(SubmissionLocalComment.PartialChange.BYLINE)
        } else {
          // In case of comment collapse, a full re-bind is required AND the
          // change animation also needs to be played to show change in body size.
          null
        }
      }

      SubmissionCommentRowType.COMMENT_OPTIONS -> {
        val oldOptions = oldItem as SubmissionCommentOptions.UiModel
        val newOptions = newItem as SubmissionCommentOptions.UiModel

        val partialChanges = ArrayList<SubmissionCommentOptions.PartialChange>(2)

        if (oldOptions.abbreviatedCount() != newOptions.abbreviatedCount()) {
          partialChanges.add(SubmissionCommentOptions.PartialChange.COMMENT_COUNT)
        }
        if (oldOptions.sortRes() != newOptions.sortRes()) {
          partialChanges.add(SubmissionCommentOptions.PartialChange.SORTING_MODE)
        }
        return partialChanges
      }

      else -> return null
    }
  }

  companion object {

    fun create(oldComments: List<SubmissionScreenUiModel>, newComments: List<SubmissionScreenUiModel>): CommentsItemDiffer {
      return CommentsItemDiffer(oldComments, newComments)
    }
  }
}
