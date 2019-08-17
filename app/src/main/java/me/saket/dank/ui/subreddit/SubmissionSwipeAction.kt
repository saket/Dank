package me.saket.dank.ui.subreddit

import androidx.annotation.StringRes
import me.saket.dank.R

enum class SubmissionSwipeAction(@StringRes val displayNameRes: Int) {
  Options(R.string.subreddit_submission_swipe_action_options),
  NewTab(R.string.subreddit_submission_swipe_action_new_tab),
  Save(R.string.subreddit_submission_swipe_action_save),
  Upvote(R.string.subreddit_submission_swipe_action_upvote),
  Downvote(R.string.subreddit_submission_swipe_action_downvote)
}
