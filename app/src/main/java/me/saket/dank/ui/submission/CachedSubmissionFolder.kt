package me.saket.dank.ui.submission

import java.io.Serializable

/**
 * Uniquely identifies a cached submission by its subreddit's name and sorting information.
 */
data class CachedSubmissionFolder(
    @get:JvmName("subredditName")
    val subredditName: String,

    @get:JvmName("sortingAndTimePeriod")
    val sortingAndTimePeriod: SortingAndTimePeriod
) : Serializable
