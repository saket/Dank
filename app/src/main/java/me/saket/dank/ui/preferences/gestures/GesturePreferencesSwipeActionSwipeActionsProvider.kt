package me.saket.dank.ui.preferences.gestures

import androidx.annotation.CheckResult
import androidx.annotation.StringRes
import com.jakewharton.rxrelay2.PublishRelay
import me.saket.dank.R
import me.saket.dank.widgets.swipe.*
import me.saket.dank.widgets.swipe.SwipeTriggerRippleDrawable.RippleType
import me.saket.dank.widgets.swipe.SwipeableLayout.SwipeActionIconProvider
import javax.inject.Inject

/**
 * Provides swipe actions for gesture swipe action preferences.
 */
class GesturePreferencesSwipeActionSwipeActionsProvider<T : Any> @Inject constructor() : SwipeActionIconProvider {
  private val swipeActions: SwipeActions
  private val deleteSwipeActions = PublishRelay.create<ActionWithDirection<T>>()

  init {
    val deleteAction = SwipeAction.create(ACTION_NAME_DELETE, R.color.appshortcut_swipe_delete, 1f)
    val actionsHolder = SwipeActionsHolder.builder()
      .add(deleteAction)
      .build()
    swipeActions = SwipeActions.builder()
      .startActions(actionsHolder)
      .endActions(actionsHolder)
      .build()
  }

  fun actions(): SwipeActions {
    return swipeActions
  }

  @CheckResult
  fun streamDeletes(): PublishRelay<ActionWithDirection<T>> = deleteSwipeActions

  override fun showSwipeActionIcon(imageView: SwipeActionIconView, oldAction: SwipeAction?, newAction: SwipeAction) {
    if (newAction.labelRes() == ACTION_NAME_DELETE) {
      imageView.setImageResource(R.drawable.ic_delete_20dp)

    } else {
      throw AssertionError("Unknown swipe action: $newAction")
    }
  }

  fun performSwipeAction(
    swipeAction: SwipeAction,
    item: ActionWithDirection<T>,
    swipeableLayout: SwipeableLayout,
    swipeDirection: SwipeDirection
  ) {
    when (swipeAction.labelRes()) {
      ACTION_NAME_DELETE -> {
        deleteSwipeActions.accept(item)
      }

      else -> throw AssertionError("Unknown swipe action: $swipeAction")
    }
    swipeableLayout.playRippleAnimation(swipeAction, RippleType.REGISTER, swipeDirection)
  }

  data class ActionWithDirection<T>(val swipeAction: T, val isStartAction: Boolean)

  companion object {
    @StringRes
    private val ACTION_NAME_DELETE = R.string.appshrotcuts_swipe_action_delete
  }
}
