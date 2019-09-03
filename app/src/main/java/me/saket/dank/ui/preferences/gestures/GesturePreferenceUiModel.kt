package me.saket.dank.ui.preferences.gestures

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

interface GesturePreferenceUiModel {

  enum class Type {
    SUBMISSION_PREVIEW,
    SECTION_HEADER,
    SWIPE_ACTION,
    SWIPE_ACTION_PLACEHOLDER
  }

  fun adapterId(): Long

  fun type(): Type

  interface ChildAdapter<T : GesturePreferenceUiModel, VH : RecyclerView.ViewHolder> {
    fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): VH

    fun onBindViewHolder(holder: VH, uiModel: T)

    fun onBindViewHolder(holder: VH, uiModel: T, payloads: List<Any>)
  }
}
