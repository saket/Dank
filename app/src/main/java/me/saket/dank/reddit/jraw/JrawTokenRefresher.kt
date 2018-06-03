package me.saket.dank.reddit.jraw

import android.text.format.DateUtils
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers.io
import io.reactivex.subjects.BehaviorSubject
import me.saket.dank.reddit.jraw.JrawTokenRefresher.TokenStatus.FRESH
import me.saket.dank.reddit.jraw.JrawTokenRefresher.TokenStatus.REFRESH_AHEAD_OF_TIME
import me.saket.dank.reddit.jraw.JrawTokenRefresher.TokenStatus.REFRESH_IMMEDIATELY
import me.saket.dank.utils.Arrays2
import net.dean.jraw.RedditClient
import net.dean.jraw.android.SharedPreferencesTokenStore
import net.dean.jraw.models.OAuthData
import net.dean.jraw.models.PersistedAuthData
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
class JrawTokenRefresher @Inject constructor(
    private val clients: BehaviorSubject<RedditClient>,
    private val tokenStore: SharedPreferencesTokenStore
) : Interceptor {

  private var isAheadOfTimeRefreshInFlight: Boolean = false
  private val inFlightImmediateRefreshes: MutableMap<Long, Completable> = Arrays2.hashMap(3)

  override fun intercept(chain: Interceptor.Chain): Response {
    if (tokenStore.usernames.isEmpty()) {
      Timber.i("Token store usernames is empty")
      return chain.proceed(chain.request())
    }

    val expirationDate = tokenExpirationDate()
    val recommendedRefreshDate = expirationDate.minusMinutes(10)

    Timber.w("Expiration date: %s", expirationDate)
    Timber.w("Time till token expiration: ${formatTime(expirationDate)}")
    Timber.w("Time till pro-active token expiration: ${formatTime(recommendedRefreshDate)}")

    val refresh: Completable = when (computeTokenStatus(expirationDate, recommendedRefreshDate)) {
      REFRESH_AHEAD_OF_TIME -> refreshAheadOfTime(recommendedRefreshDate)
      REFRESH_IMMEDIATELY -> refreshImmediately(expirationDate)
      FRESH -> Completable.complete()
    }

    refresh.blockingAwait()

    return chain.proceed(chain.request())
  }

  private fun refreshImmediately(expirationDate: LocalDateTime): Completable {
    val key = expirationDate.toEpochSecond(UTC)

    Timber.w("Refreshing immediately")

    if (!inFlightImmediateRefreshes.containsKey(key)) {
      val cachedRefreshStream = clients
          .take(1)
          .map { it.authManager }
          .map { it.renew() }
          .ignoreElements()
          .cache()

      cachedRefreshStream
          .doOnSubscribe { inFlightImmediateRefreshes[key] = cachedRefreshStream }
          .subscribe { inFlightImmediateRefreshes.remove(key) }
    }

    return inFlightImmediateRefreshes[key]!!
  }

  private fun refreshAheadOfTime(recommendedRefreshDate: LocalDateTime): Completable {
    Timber.w("Time to refresh token")
    Timber.w("Recommended refresh date: $recommendedRefreshDate")
    Timber.w("Now time: ${LocalDateTime.now(UTC)}")

    if (isAheadOfTimeRefreshInFlight) {
      return Completable.complete()
    }

    return clients
        .take(1)
        .map { it.authManager }
        .filter { it.canRenew() }
        .map { it.renew() }
        .subscribeOn(io())
        .ignoreElements()
        .doOnSubscribe { isAheadOfTimeRefreshInFlight = true }
        .doOnComplete {
          isAheadOfTimeRefreshInFlight = false
          Timber.w("Token refreshed")
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

  private fun tokenExpirationDate(): LocalDateTime {
    val loggedInUsername = tokenStore.usernames.first()
    val persistedAuthData: PersistedAuthData = tokenStore.inspect(loggedInUsername)!!
    val latestOAuthData: OAuthData = persistedAuthData.latest!!
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
