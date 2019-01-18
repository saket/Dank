package me.saket.dank.ui.post

import me.saket.dank.ui.UiEvent

data class NewPostSubredditSelected(val subredditName: String) : UiEvent

data class NewPostTitleTextChanged(val text: String) : UiEvent

data class NewPostBodyTextChanged(val text: String) : UiEvent

object NewPostBodyBackspacePressed : UiEvent

data class NewPostImageSelectionUpdated(val images: List<ImageToUpload>) : UiEvent

object NewPostSendClicked : UiEvent

data class NewPostOptionsUpdated(val options: NewPostOptions) : UiEvent
