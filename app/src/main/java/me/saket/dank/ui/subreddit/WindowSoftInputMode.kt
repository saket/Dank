package me.saket.dank.ui.subreddit

import android.view.WindowManager

enum class WindowSoftInputMode(val frameworkValue: Int) {
  ADJUST_NOTHING(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING),
  ADJUST_RESIZE(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
}
