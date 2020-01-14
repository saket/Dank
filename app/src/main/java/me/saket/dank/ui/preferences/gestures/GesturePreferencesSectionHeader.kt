package me.saket.dank.ui.preferences.gestures

import android.view.View
import me.saket.dank.ui.preferences.adapter.UserPreferenceSectionHeader
import javax.inject.Inject

interface GesturePreferencesSectionHeader {

  data class UiModel(val label: String) : GesturePreferenceUiModel {
    val headerUiModel: UserPreferenceSectionHeader.UiModel = UserPreferenceSectionHeader.UiModel.create(label)

    override fun adapterId() = headerUiModel.adapterId()

    override fun type() = GesturePreferenceUiModel.Type.SECTION_HEADER
  }

  class ViewHolder(itemView: View) : UserPreferenceSectionHeader.ViewHolder(itemView)

  class Adapter @Inject constructor() : UserPreferenceSectionHeader.Adapter(),
    GesturePreferenceUiModel.ChildAdapter<UiModel, UserPreferenceSectionHeader.ViewHolder> {

    override fun onBindViewHolder(
      holder: UserPreferenceSectionHeader.ViewHolder,
      uiModel: UiModel
    ) {
      super.onBindViewHolder(holder, uiModel.headerUiModel)
    }

    override fun onBindViewHolder(
      holder: UserPreferenceSectionHeader.ViewHolder,
      uiModel: UiModel,
      payloads: List<Any>
    ) {
      throw UnsupportedOperationException()
    }
  }
}
