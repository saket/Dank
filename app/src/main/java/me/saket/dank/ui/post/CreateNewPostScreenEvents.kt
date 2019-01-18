package me.saket.dank.ui.post

import me.saket.dank.ui.UiEvent

data class NewPostSubredditSelected(val subredditName: String) : UiEvent

data class NewPostTitleTextChanged(val title: String) : UiEvent

data class NewPostBodyTextChanged(val body: String) : UiEvent

object NewPostBodyBackspaceClicked : UiEvent

data class NewPostImageSelectionUpdated(val images: List<ImageToUpload>) : UiEvent

object NewPostSubmitClicked : UiEvent

data class NewPostOptionsUpdated(val options: NewPostOptions) : UiEvent
