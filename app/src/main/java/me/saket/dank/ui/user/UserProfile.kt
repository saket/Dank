package me.saket.dank.ui.user

import com.squareup.moshi.JsonClass
import net.dean.jraw.models.Account

sealed class UserProfileSearchResult

@JsonClass(generateAdapter = true)
data class UserProfile(
    val account: Account,
    val userSubreddit: UserSubreddit?
) : UserProfileSearchResult()

object UserNotFound : UserProfileSearchResult()

object UserSuspended : UserProfileSearchResult()

data class UnexpectedError(val error: Throwable) : UserProfileSearchResult()
