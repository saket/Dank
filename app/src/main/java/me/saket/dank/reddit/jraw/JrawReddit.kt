package me.saket.dank.reddit.jraw

import io.reactivex.subjects.BehaviorSubject
import me.saket.dank.reddit.Reddit
import me.saket.dank.ui.user.UserSessionRepository
import net.dean.jraw.RedditClient
import net.dean.jraw.android.SharedPreferencesTokenStore
import net.dean.jraw.http.LogAdapter
import net.dean.jraw.http.SimpleHttpLogger
import net.dean.jraw.oauth.AccountHelper
import timber.log.Timber
import javax.inject.Inject

class JrawReddit @Inject constructor(
    userSessionRepository: UserSessionRepository,
    private val accountHelper: AccountHelper,
    private val clientSubject: BehaviorSubject<RedditClient>,
    tokenStore: SharedPreferencesTokenStore
) : Reddit {

  init {
    accountHelper.onSwitch { newRedditClient -> clientSubject.onNext(newRedditClient) }

    Timber.i("Existing usernames: ${tokenStore.usernames}")
    when {
      userSessionRepository.isUserLoggedIn -> {
        val loggedInUserName = userSessionRepository.loggedInUserName()!!

        if (!tokenStore.usernames.contains(loggedInUserName)) {
          throw AssertionError("Logged-in username ($loggedInUserName) in session repository is not contained in JRAW's store")
        }

        Timber.i("Switching to user: $loggedInUserName")
        accountHelper.switchToUser(loggedInUserName)
      }
      else -> {
        Timber.i("Switching to userless")
        accountHelper.switchToUserless()
      }
    }

    clientSubject
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
    return JrawSubmissions(clientSubject)
  }

  override fun subreddits(): Reddit.Subreddits {
    return JrawSubreddits(clientSubject)
  }

  override fun subscriptions(): Reddit.Subscriptions {
    return JrawSubscriptions(clientSubject)
  }

  override fun loggedInUser(): Reddit.LoggedInUser {
    return JrawLoggedInUser(clientSubject, accountHelper)
  }

  override fun users(): Reddit.Users {
    return JrawUsers(clientSubject)
  }

  override fun login(): Reddit.Logins {
    return JrawLogins(accountHelper)
  }
}
