package me.saket.dank.reddit.jraw

import com.jakewharton.rxrelay2.BehaviorRelay
import me.saket.dank.reddit.Reddit
import net.dean.jraw.RedditClient
import net.dean.jraw.android.SharedPreferencesTokenStore
import net.dean.jraw.http.LogAdapter
import net.dean.jraw.http.SimpleHttpLogger
import net.dean.jraw.oauth.AccountHelper
import timber.log.Timber
import javax.inject.Inject


class JrawReddit @Inject constructor(
    private val accountHelper: AccountHelper,
    tokenStore: SharedPreferencesTokenStore
) : Reddit {

  private val redditClientProvider = BehaviorRelay.create<RedditClient>()

  init {
    accountHelper.onSwitch { newRedditClient -> redditClientProvider.accept(newRedditClient) }
    Timber.i("Existing usernames: ${tokenStore.usernames}")
    when {
      tokenStore.usernames.isNotEmpty() -> {
        Timber.i("Switching to user")
        accountHelper.switchToUser(tokenStore.usernames.first())
      }
      else -> {
        Timber.i("Switching to userless")
        accountHelper.switchToUserless()
      }
    }

    redditClientProvider
        .subscribe { client ->
          // By default, JRAW logs HTTP activity to System.out.
          val logAdapter: LogAdapter = object : LogAdapter {
            override fun writeln(data: String) {
              Timber.tag("Reddit").d(data)
            }
          }
          client.logger = SimpleHttpLogger(SimpleHttpLogger.DEFAULT_LINE_LENGTH, logAdapter)
        }
  }

  override fun submissions(): Reddit.Submissions {
    return JrawSubmissions(redditClientProvider)
  }

  override fun subreddits(): Reddit.Subreddits {
    return JrawSubreddits(redditClientProvider)
  }

  override fun subscriptions(): Reddit.Subscriptions {
    return JrawSubscriptions(redditClientProvider)
  }

  override fun loggedInUser(): Reddit.LoggedInUser {
    return JrawLoggedInUser(redditClientProvider, accountHelper)
  }

  override fun users(): Reddit.Users {
    return JrawUsers(redditClientProvider)
  }

  override fun login(): Reddit.Logins {
    return JrawLogins(accountHelper)
  }
}
