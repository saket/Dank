package me.saket.dank.ui.preferences.gestures

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import me.saket.dank.ui.preferences.gestures.submissions.GesturePreferencesSubmissionPreview
import me.saket.dank.ui.subreddit.SubmissionSwipeAction
import me.saket.dank.utils.Pair
import me.saket.dank.utils.RecyclerViewArrayAdapter
import javax.inject.Inject

class GesturePreferencesAdapter @Inject constructor(
  submissionPreviewAdapter: GesturePreferencesSubmissionPreview.Adapter,
  sectionHeaderAdapter: GesturePreferencesSectionHeader.Adapter,
  private val swipeActionAdapter: GesturePreferencesSwipeAction.Adapter,
  private val swipeActionPlaceholderAdapter: GesturePreferencesSwipeActionPlaceholder.Adapter
) : RecyclerViewArrayAdapter<GesturePreferenceUiModel, RecyclerView.ViewHolder>(),
  Consumer<Pair<List<GesturePreferenceUiModel>, DiffUtil.DiffResult>> {

  private val childAdapters = mapOf(
    GesturePreferenceUiModel.Type.SUBMISSION_PREVIEW to submissionPreviewAdapter,
    GesturePreferenceUiModel.Type.SECTION_HEADER to sectionHeaderAdapter,
    GesturePreferenceUiModel.Type.SWIPE_ACTION to swipeActionAdapter,
    GesturePreferenceUiModel.Type.SWIPE_ACTION_PLACEHOLDER to swipeActionPlaceholderAdapter
  )

  init {
    setHasStableIds(true)
  }

  @CheckResult
  fun streamDragStarts(): Observable<GesturePreferencesSwipeAction.ViewHolder> {
    return swipeActionAdapter.streamDragStarts()
  }

  @CheckResult
  fun streamDeletes(): Observable<GesturePreferencesSwipeActionSwipeActionsProvider.ActionWithDirection<SubmissionSwipeAction>> {
    return swipeActionAdapter.streamDeletes()
  }

  @CheckResult
  fun streamAddClicks(): Observable<kotlin.Pair<View, GesturePreferencesSwipeActionPlaceholder.UiModel>> {
    return swipeActionPlaceholderAdapter.streamAddClicks()
  }

  override fun getItemViewType(position: Int): Int {
    return getItem(position).type().ordinal
  }

  override fun getItemId(position: Int): Long {
    return getItem(position).adapterId()
  }

  override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    return childAdapters.getValue(VIEW_TYPES[viewType]).onCreateViewHolder(inflater, parent)
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    @Suppress("UNCHECKED_CAST")
    (childAdapters[VIEW_TYPES[holder.itemViewType]]
        as GesturePreferenceUiModel.ChildAdapter<GesturePreferenceUiModel, RecyclerView.ViewHolder>)
      .onBindViewHolder(holder, getItem(position))
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
    if (payloads.isEmpty()) {
      onBindViewHolder(holder, position)
    } else {
      @Suppress("UNCHECKED_CAST")
      (childAdapters[VIEW_TYPES[holder.itemViewType]]
          as GesturePreferenceUiModel.ChildAdapter<GesturePreferenceUiModel, RecyclerView.ViewHolder>)
        .onBindViewHolder(holder, getItem(position), payloads)
    }
  }

  override fun accept(pair: Pair<List<GesturePreferenceUiModel>, DiffUtil.DiffResult>) {
    updateData(pair.first())
    pair.second().dispatchUpdatesTo(this)
  }

  companion object {
    private val VIEW_TYPES = GesturePreferenceUiModel.Type.values()
  }
}
