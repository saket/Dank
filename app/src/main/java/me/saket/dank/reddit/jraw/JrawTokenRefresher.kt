package me.saket.dank.reddit.jraw

import android.text.format.DateUtils
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers.io
import io.reactivex.subjects.BehaviorSubject
import me.saket.dank.BuildConfig
import me.saket.dank.reddit.jraw.JrawTokenRefresher.TokenStatus.FRESH
import me.saket.dank.reddit.jraw.JrawTokenRefresher.TokenStatus.REFRESH_AHEAD_OF_TIME
import me.saket.dank.reddit.jraw.JrawTokenRefresher.TokenStatus.REFRESH_IMMEDIATELY
import net.dean.jraw.RedditClient
import net.dean.jraw.models.OAuthData
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
    if (chain.request().url().toString().startsWith("https://www.reddit.com/api/v1/access_token")) {
      return chain.proceed(chain.request())
    }

    val canRenew = clients
        .take(1)
        .map { it.authManager }
        .map { it.canRenew() }
        .onErrorReturnItem(false)
        .blockingFirst()

    if (!canRenew) {
      log("Token store usernames is empty")
      return chain.proceed(chain.request())
    }

    return clients
        .take(1)
        .flatMapCompletable { client ->
          val expirationDate = tokenExpirationDate(client)
          val recommendedRefreshDate = expirationDate.minusMinutes(10)

          log("Expiration date: $expirationDate")
          log("Time till token expiration: ${formatTime(expirationDate)}")
          log("Username: ${client.authManager.currentUsername()}")
          log("Time till pro-active token expiration: ${formatTime(recommendedRefreshDate)}")

          when (computeTokenStatus(expirationDate, recommendedRefreshDate)) {
            REFRESH_AHEAD_OF_TIME -> refreshAheadOfTime(recommendedRefreshDate)
            REFRESH_IMMEDIATELY -> refreshImmediately()
            FRESH -> Completable.complete()
          }
        }
        .andThen(Single.fromCallable { chain.proceed(chain.request()) })
        .blockingGet()
  }

  private fun refreshImmediately(): Completable {
    throw AssertionError("Jraw did not refresh token automatically")
  }

  private fun refreshAheadOfTime(recommendedRefreshDate: LocalDateTime): Completable {
    log("Time to refresh token")
    log("Recommended refresh date: $recommendedRefreshDate")
    log("Now time: ${LocalDateTime.now(UTC)}")

    if (isAheadOfTimeRefreshInFlight) {
      return Completable.complete()
    }

    return clients
        .take(1)
        .map { it.authManager }
        .filter { it.canRenew() }
        .flatMapCompletable {
          Completable.fromAction { it.renew() }
              .subscribeOn(io())
              .onErrorComplete()
        }
        .doOnSubscribe { isAheadOfTimeRefreshInFlight = true }
        .doOnComplete {
          isAheadOfTimeRefreshInFlight = false
          log("Token refreshed")
        }
  }

  enum class TokenStatus {
    REFRESH_AHEAD_OF_TIME,
    REFRESH_IMMEDIATELY,
    FRESH
  }

  private fun computeTokenStatus(expirationDate: LocalDateTime, recommendedRefreshDate: LocalDateTime): TokenStatus {
    val now = LocalDateTime.now(UTC)

    if (now > recommendedRefreshDate && now < expirationDate) {
      return REFRESH_AHEAD_OF_TIME
    }

    if (now >= expirationDate) {
      return REFRESH_IMMEDIATELY
    }

    val needsRefresh = clients
        .take(1)
        .map { it.authManager }
        .map { it.needsRenewing() }
        .blockingFirst()
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
