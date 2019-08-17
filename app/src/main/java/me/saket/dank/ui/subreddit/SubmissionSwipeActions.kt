package me.saket.dank.ui.subreddit

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import me.saket.dank.R
import me.saket.dank.widgets.swipe.SwipeAction

object SubmissionSwipeActions {
  private val moreOptionsSwipeAction = SwipeAction.create(SubmissionSwipeAction.Options.displayNameRes, R.color.list_item_swipe_more_options, 0.5f)
  private val newTabSwipeAction = SwipeAction.create(SubmissionSwipeAction.NewTab.displayNameRes, R.color.list_item_swipe_new_tab, 0.5f)
  private val saveSwipeAction = SwipeAction.create(SubmissionSwipeAction.Save.displayNameRes, R.color.list_item_swipe_save, 0.5f)
  private val upvoteSwipeAction = SwipeAction.create(SubmissionSwipeAction.Upvote.displayNameRes, R.color.list_item_swipe_upvote, 0.5f)
  private val downvoteSwipeAction = SwipeAction.create(SubmissionSwipeAction.Downvote.displayNameRes, R.color.list_item_swipe_downvote, 0.5f)

  val all by lazy {
    listOf(
      moreOptionsSwipeAction,
      newTabSwipeAction,
      saveSwipeAction,
      upvoteSwipeAction,
      downvoteSwipeAction
    )
  }

  @DrawableRes
  fun getSwipeActionIconRes(swipeAction: SubmissionSwipeAction): Int {
    return getSwipeActionIconRes(swipeAction.displayNameRes)
  }

  @DrawableRes
  fun getSwipeActionIconRes(@StringRes swipeActionDisplayNameRes: Int): Int {
    return when (swipeActionDisplayNameRes) {
      SubmissionSwipeAction.Options.displayNameRes -> R.drawable.ic_more_horiz_20dp
      SubmissionSwipeAction.NewTab.displayNameRes -> R.drawable.ic_open_in_new_tab_20dp
      SubmissionSwipeAction.Save.displayNameRes -> R.drawable.ic_save_20dp
      SubmissionSwipeAction.Upvote.displayNameRes -> R.drawable.ic_arrow_upward_20dp
      SubmissionSwipeAction.Downvote.displayNameRes -> R.drawable.ic_arrow_downward_20dp

      else -> throw UnsupportedOperationException("Unknown swipe action displayNameRes: $swipeActionDisplayNameRes")
    }
  }

  fun getActionByLabel(labelRes: Int): SubmissionSwipeAction {
    return when (labelRes) {
      SubmissionSwipeAction.Options.displayNameRes -> SubmissionSwipeAction.Options
      SubmissionSwipeAction.NewTab.displayNameRes -> SubmissionSwipeAction.NewTab
      SubmissionSwipeAction.Save.displayNameRes -> SubmissionSwipeAction.Save
      SubmissionSwipeAction.Upvote.displayNameRes -> SubmissionSwipeAction.Upvote
      SubmissionSwipeAction.Downvote.displayNameRes -> SubmissionSwipeAction.Downvote

      else -> throw UnsupportedOperationException("Unknown swipe action labelRes: $labelRes")
    }
  }

  fun getAction(action: SubmissionSwipeAction): SwipeAction {
    return when (action) {
      SubmissionSwipeAction.Options -> moreOptionsSwipeAction
      SubmissionSwipeAction.NewTab -> newTabSwipeAction
      SubmissionSwipeAction.Save -> saveSwipeAction
      SubmissionSwipeAction.Upvote -> upvoteSwipeAction
      SubmissionSwipeAction.Downvote -> downvoteSwipeAction
    }
  }
}
