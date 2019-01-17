package me.saket.dank.ui.subreddit

import me.saket.dank.ui.UiEvent
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout

data class SubredditChanged(val subredditName: String) : UiEvent

data class SubredditListScrolled(val dy: Int) : UiEvent {
  /** RecyclerView emits a 0 dy scroll event when the data-set is refreshed. */
  val isListRefreshEvent = dy == 0
}

data class SubmissionPageStateChanged(val pageState: ExpandablePageLayout.PageState) : UiEvent
