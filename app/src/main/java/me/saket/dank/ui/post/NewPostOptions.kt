package me.saket.dank.ui.post

data class NewPostOptions(
    val isNsfw: Boolean,
    val hasSpoilers: Boolean,
    val sendRepliesToInbox: Boolean,
    val flair: String
)
