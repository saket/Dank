package me.saket.dank.ui.preferences.gestures

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.CheckResult
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.jakewharton.rxrelay2.PublishRelay
import com.jakewharton.rxrelay2.Relay
import dagger.Lazy
import io.reactivex.Observable
import me.saket.dank.R
import me.saket.dank.ui.subreddit.SubmissionSwipeAction
import me.saket.dank.utils.ItemTouchHelperDragAndDropCallback
import me.saket.dank.utils.Views
import me.saket.dank.widgets.swipe.SwipeableLayout
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions
import javax.inject.Inject

interface GesturePreferencesSwipeAction {

  data class UiModel(
    val swipeAction: SubmissionSwipeAction,
    val isStartAction: Boolean,
    @DrawableRes val iconRes: Int
  ) : GesturePreferenceUiModel {
    override fun adapterId() = hashCode().toLong()

    override fun type() = GesturePreferenceUiModel.Type.SWIPE_ACTION
  }

  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), ViewHolderWithSwipeActions,
    ItemTouchHelperDragAndDropCallback.DraggableViewHolder {

    @BindView(R.id.item_gesturepreferences_swipeaction_label)
    lateinit var labelView: TextView

    @BindView(R.id.item_gesturepreferences_swipeaction_swipeable_layout)
    lateinit var swipeableLayoutView: SwipeableLayout

    @BindView(R.id.item_gesturepreferences_swipeaction_drag)
    lateinit var dragButton: ImageButton

    lateinit var uiModel: UiModel

    init {
      ButterKnife.bind(this, itemView)
    }

    fun setupDeleteGesture(swipeActionsProvider: GesturePreferencesSwipeActionSwipeActionsProvider<SubmissionSwipeAction>) {
      swipeableLayoutView.setSwipeActionIconProvider(swipeActionsProvider)
      swipeableLayoutView.setSwipeActions(swipeActionsProvider.actions())
      swipeableLayoutView.setOnPerformSwipeActionListener { action, swipeDirection ->
        val actionWithDirection = GesturePreferencesSwipeActionSwipeActionsProvider.ActionWithDirection(uiModel.swipeAction, uiModel.isStartAction)
        swipeActionsProvider.performSwipeAction(action, actionWithDirection, swipeableLayoutView, swipeDirection)
      }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupDragGesture(dragStarts: Relay<ViewHolder>) {
      dragButton.setOnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
          dragStarts.accept(this)
        }
        dragButton.onTouchEvent(event)
      }
    }

    fun render() {
      labelView.setText(uiModel.swipeAction.displayNameRes)
      Views.setCompoundDrawableStart(labelView, uiModel.iconRes)
    }

    override fun onDragStart() {
      swipeableLayoutView.animate()
        .translationZ(swipeableLayoutView.resources.getDimensionPixelSize(R.dimen.elevation_recyclerview_row_drag_n_drop).toFloat())
        .setDuration(100)
        .start()
    }

    override fun onDragEnd() {
      swipeableLayoutView.animate()
        .translationZ(0f)
        .setDuration(50)
        .start()
    }

    override fun getSwipeableLayout() = swipeableLayoutView
  }

  class Adapter @Inject constructor(
    private val swipeActionsProvider: Lazy<GesturePreferencesSwipeActionSwipeActionsProvider<SubmissionSwipeAction>>
  ) : GesturePreferenceUiModel.ChildAdapter<UiModel, ViewHolder> {

    private val dragStarts = PublishRelay.create<ViewHolder>()

    @CheckResult
    fun streamDeletes(): Observable<GesturePreferencesSwipeActionSwipeActionsProvider.ActionWithDirection<SubmissionSwipeAction>> = swipeActionsProvider.get().streamDeletes()

    @CheckResult
    fun streamDragStarts(): Observable<ViewHolder> = dragStarts

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
      val holder = ViewHolder(inflater.inflate(R.layout.list_item_gesture_preference_swipe_action, parent, false))
      holder.setupDeleteGesture(swipeActionsProvider.get())
      holder.setupDragGesture(dragStarts)
      return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, uiModel: UiModel) {
      holder.uiModel = uiModel
      holder.render()
    }

    override fun onBindViewHolder(holder: ViewHolder, uiModel: UiModel, payloads: List<Any>) {
      throw UnsupportedOperationException()
    }
  }
}
