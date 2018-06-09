package me.saket.dank.reddit.jraw

import io.reactivex.Completable
import me.saket.dank.reddit.Reddit
import net.dean.jraw.oauth.AccountHelper
import net.dean.jraw.oauth.StatefulAuthHelper

class JrawLogins(private val accountHelper: AccountHelper) : Reddit.Logins {

  override fun loginHelper(): UserLoginHelper {
    return UserLoginHelper(accountHelper.switchToNewUser())
  }
}

class UserLoginHelper(private val helper: StatefulAuthHelper) {

  fun authorizationUrl(): String {
    val scopes = arrayOf(
        "account",
        "edit", // For editing comments and submissions
        "history",
        "identity",
        "mysubreddits",
        "privatemessages",
        "read",
        "report", // For hiding or reporting a thread.
        "save",
        "submit",
        "subscribe",
        "vote",
        "wikiread")
    return helper.getAuthorizationUrl(requestRefreshToken = true, useMobileSite = true, scopes = *scopes)
  }

  /**
   * Emits an item when the app is successfully able to authenticate the user in.
   */
  fun parseOAuthSuccessUrl(successUrl: String): Completable {
    return Completable.fromAction { helper.onUserChallenge(successUrl) }
  }
}
