package me.saket.dank.ui.preferences.gestures

import android.support.v7.util.DiffUtil
import me.saket.dank.ui.preferences.gestures.submissions.GesturePreferencesSubmissionPreview
import me.saket.dank.ui.subreddit.uimodels.SubmissionItemDiffer

object GesturePreferenceItemDiffer : DiffUtil.ItemCallback<GesturePreferenceUiModel>() {
  override fun areItemsTheSame(oldItem: GesturePreferenceUiModel, newItem: GesturePreferenceUiModel): Boolean {
    if (oldItem is GesturePreferencesSubmissionPreview.UiModel && newItem is GesturePreferencesSubmissionPreview.UiModel) {
      return SubmissionItemDiffer.areItemsTheSame(oldItem.submissionUiModel, newItem.submissionUiModel)
    }
    return oldItem.adapterId() == newItem.adapterId()
  }

  override fun areContentsTheSame(oldItem: GesturePreferenceUiModel, newItem: GesturePreferenceUiModel): Boolean {
    if (oldItem is GesturePreferencesSubmissionPreview.UiModel && newItem is GesturePreferencesSubmissionPreview.UiModel) {
      return SubmissionItemDiffer.areContentsTheSame(oldItem.submissionUiModel, newItem.submissionUiModel)
    }
    return oldItem == newItem
  }

  override fun getChangePayload(oldItem: GesturePreferenceUiModel, newItem: GesturePreferenceUiModel): Any? {
    if (oldItem is GesturePreferencesSubmissionPreview.UiModel && newItem is GesturePreferencesSubmissionPreview.UiModel) {
      return SubmissionItemDiffer.getChangePayload(oldItem.submissionUiModel, newItem.submissionUiModel)
    }
    return super.getChangePayload(oldItem, newItem)
  }
}
