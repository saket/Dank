package me.saket.dank.ui.post

data class PostToSubmit(
    val title: String,
    val body: String,
    val options: NewPostOptions
)
