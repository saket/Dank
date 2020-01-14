package me.saket.dank.ui.submission.events

import android.graphics.Point
import android.view.Gravity
import android.view.View
import me.saket.dank.R
import me.saket.dank.data.SwipeEvent
import me.saket.dank.ui.submission.CommentOptionsPopup
import me.saket.dank.utils.Views
import me.saket.dank.widgets.swipe.SwipeableLayout
import net.dean.jraw.models.Comment
import kotlin.math.max

data class CommentOptionSwipeEvent(private val comment: Comment, private val itemView: SwipeableLayout) : SwipeEvent {

  fun showPopup(toolbar: View) {
    val commentLayout = itemView
    val sheetLocation = Views.locationOnScreen(commentLayout)
    val popupLocation = Point(0, sheetLocation.y)

    // Align with comment body.
    val resources = itemView.resources
    popupLocation.offset(
        resources.getDimensionPixelSize(R.dimen.submission_comment_horiz_padding),
        resources.getDimensionPixelSize(R.dimen.submission_comment_top_padding))

    // Keep below toolbar.
    val toolbarBottom = Views.locationOnScreen(toolbar).y + toolbar.bottom + resources.getDimensionPixelSize(R.dimen.spacing16)
    popupLocation.y = max(popupLocation.y, toolbarBottom)

    val optionsPopup = CommentOptionsPopup(commentLayout.context, comment)
    optionsPopup.showAtLocation(commentLayout, Gravity.TOP or Gravity.START, popupLocation)
  }
}
