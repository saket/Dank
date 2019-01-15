package me.saket.dank.ui.subreddit

import me.saket.dank.ui.UiEvent

data class SubredditChanged(val subredditName: String) : UiEvent
