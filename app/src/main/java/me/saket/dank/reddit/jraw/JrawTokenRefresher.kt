package me.saket.dank.reddit.jraw

import android.text.format.DateUtils
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers.io
import io.reactivex.subjects.BehaviorSubject
import me.saket.dank.BuildConfig
import me.saket.dank.reddit.jraw.JrawTokenRefresher.TokenStatus.FRESH
import me.saket.dank.reddit.jraw.JrawTokenRefresher.TokenStatus.REFRESH_AHEAD_OF_TIME
import net.dean.jraw.RedditClient
import net.dean.jraw.android.SharedPreferencesTokenStore
import net.dean.jraw.models.OAuthData
import net.dean.jraw.oauth.AuthMethod
import okhttp3.Interceptor
import okhttp3.Response
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset.UTC
import timber.log.Timber
import javax.inject.Inject

/**
 * JRAW is capable of refreshing tokens automatically, but it doesn't seem to work.
 * This class does that + also refreshes tokens ahead of time (asynchronously) to
 * avoid making the user wait longer.
 */
class JrawTokenRefresher @Inject constructor(private val clients: BehaviorSubject<RedditClient>) : Interceptor {

  private var isAheadOfTimeRefreshInFlight: Boolean = false

  fun log(msg: String) {
    if (BuildConfig.DEBUG) {
      Timber.w(msg)
    }
  }

  override fun intercept(chain: Interceptor.Chain): Response {
    if (chain.request().url.toString().startsWith("https://www.reddit.com/api/v1/access_token")) {
      return chain.proceed(chain.request())
    }

    clients
        .take(1)
        .flatMapCompletable {
          fixRefreshToken(it)
              .andThen(renewIfNeeded(it))
        }
        .onErrorComplete()
        .subscribe()

    return chain.proceed(chain.request())
  }

  /**
   * https://github.com/mattbdean/JRAW/issues/264
   */
  private fun fixRefreshToken(client: RedditClient): Completable {
    if (client.authMethod != AuthMethod.APP) {
      return Completable.complete()
    }

    return Completable.fromAction {
      val authManager = client.authManager
      val tokenStore = authManager.tokenStore
      val currentUsername = client.authManager.currentUsername()

      val storedAuthData: OAuthData? = tokenStore.fetchLatest(currentUsername)
      val storedRefreshToken = tokenStore.fetchRefreshToken(currentUsername)

      val isJrawPotentiallyFuckingUp = storedAuthData != null
          && storedAuthData.refreshToken == null
          && storedRefreshToken != null

      if (isJrawPotentiallyFuckingUp) {
        Timber.w("JRAW is fucking up. Fixing refresh token.")

        tokenStore.storeLatest(
            currentUsername,
            OAuthData.create(
                storedAuthData!!.accessToken,
                storedAuthData.scopes,
                storedRefreshToken,
                storedAuthData.expiration))
      }
    }
  }

  private fun renewIfNeeded(client: RedditClient): Completable {
    if (client.authMethod != AuthMethod.APP || !client.authManager.canRenew()) {
      val usernames = (client.authManager.tokenStore as SharedPreferencesTokenStore).usernames
      log("Cannot renew. usernames: $usernames")
      return Completable.complete()
    }

    val expirationDate = tokenExpirationDate(client)
    val recommendedRefreshDate = expirationDate.minusMinutes(10)

    log("Expiration date: $expirationDate")
    log("Time till token expiration: ${formatTime(expirationDate)}")
    log("Username: ${client.authManager.currentUsername()}")
    log("Time till pro-active token expiration: ${formatTime(recommendedRefreshDate)}")

    return when (computeTokenStatus(client, expirationDate, recommendedRefreshDate)) {
      REFRESH_AHEAD_OF_TIME -> refreshAheadOfTime(client, recommendedRefreshDate)
      FRESH -> Completable.complete()
    }
  }

  private fun refreshAheadOfTime(client: RedditClient, recommendedRefreshDate: LocalDateTime): Completable {
    log("Time to refresh token")
    log("Recommended refresh date: $recommendedRefreshDate")
    log("Now time: ${LocalDateTime.now(UTC)}")

    if (isAheadOfTimeRefreshInFlight) {
      return Completable.complete()
    }

    return Completable.fromAction { client.authManager.renew() }
        .subscribeOn(io())
        .doOnSubscribe { isAheadOfTimeRefreshInFlight = true }
        .doOnComplete {
          isAheadOfTimeRefreshInFlight = false
          log("Token refreshed")
        }
  }

  enum class TokenStatus {
    REFRESH_AHEAD_OF_TIME,
    FRESH
  }

  private fun computeTokenStatus(client: RedditClient, expirationDate: LocalDateTime, recommendedRefreshDate: LocalDateTime): TokenStatus {
    val now = LocalDateTime.now(UTC)

    if (now > recommendedRefreshDate && now < expirationDate) {
      return REFRESH_AHEAD_OF_TIME
    }

    if (now >= expirationDate) {
      throw AssertionError("Jraw did not refresh token automatically")
    }

    val needsRefresh = client.authManager.needsRenewing()
    if (needsRefresh) {
      throw AssertionError("JRAW says token needs refreshing")
    }

    return FRESH
  }

  private fun tokenExpirationDate(client: RedditClient): LocalDateTime {
    val latestOAuthData: OAuthData = client.authManager.current!!
    if (latestOAuthData.expiration.time == 0L) {
      throw AssertionError("Expiration time is empty")
    }
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(latestOAuthData.expiration.time), UTC)
  }

  private fun formatTime(expirationDateTime: LocalDateTime): CharSequence? {
    return DateUtils.getRelativeTimeSpanString(
        expirationDateTime.toInstant(UTC).toEpochMilli(),
        System.currentTimeMillis(),
        0,
        DateUtils.FORMAT_ABBREV_RELATIVE or DateUtils.FORMAT_ABBREV_MONTH)
  }
}
