package me.saket.dank.ui.preferences.gestures

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.RelativeLayout
import androidx.appcompat.widget.Toolbar
import me.saket.dank.R
import me.saket.dank.ui.preferences.UserPreferenceNestedScreen

class CommentGesturesPreferenceScreen(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs),
  UserPreferenceNestedScreen {

  private lateinit var toolbar: Toolbar

  override fun onFinishInflate() {
    super.onFinishInflate()

    toolbar = findViewById(R.id.userpreferences_gestures_toolbar)
    toolbar.setTitle(R.string.userprefs_customize_comment_gestures)
  }

  override fun onInterceptPullToCollapseGesture(
    event: MotionEvent,
    downX: Float,
    downY: Float,
    upwardPagePull: Boolean
  ): Boolean {
    return false
  }

  override fun setNavigationOnClickListener(listener: OnClickListener) {
    toolbar.setNavigationOnClickListener(listener)
  }
}
