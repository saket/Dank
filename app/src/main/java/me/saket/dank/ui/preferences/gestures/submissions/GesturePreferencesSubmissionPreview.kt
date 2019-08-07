package me.saket.dank.ui.preferences.gestures.submissions

import android.view.View
import me.saket.dank.ui.preferences.gestures.GesturePreferenceUiModel
import me.saket.dank.ui.subreddit.SubmissionSwipeActionsProvider
import me.saket.dank.ui.subreddit.uimodels.SubredditSubmission
import javax.inject.Inject

interface GesturePreferencesSubmissionPreview {

  data class UiModel(val submissionUiModel: SubredditSubmission.UiModel) : GesturePreferenceUiModel {
    override fun adapterId() = submissionUiModel.adapterId()

    override fun type() = GesturePreferenceUiModel.Type.SUBMISSION_PREVIEW
  }

  class ViewHolder private constructor(itemView: View) : SubredditSubmission.ViewHolder(itemView)

  class Adapter @Inject constructor(
    swipeActionsProvider: SubmissionSwipeActionsProvider
  ) : SubredditSubmission.Adapter(swipeActionsProvider),
    GesturePreferenceUiModel.ChildAdapter<UiModel, SubredditSubmission.ViewHolder> {

    override fun onBindViewHolder(holder: SubredditSubmission.ViewHolder, uiModel: UiModel) {
      super.onBindViewHolder(holder, uiModel.submissionUiModel)
    }

    override fun onBindViewHolder(holder: SubredditSubmission.ViewHolder, uiModel: UiModel, payloads: List<Any>) {
      super.onBindViewHolder(holder, uiModel.submissionUiModel, payloads)
    }
  }
}
