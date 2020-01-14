package me.saket.dank.data

import com.bumptech.glide.load.engine.GlideException
import io.reactivex.exceptions.CompositeException
import io.reactivex.exceptions.UndeliverableException
import me.saket.dank.R
import me.saket.dank.data.exceptions.ImgurApiRequestRateLimitReachedException
import me.saket.dank.data.exceptions.ImgurApiUploadRateLimitReachedException
import okhttp3.internal.http2.ConnectionShutdownException
import okhttp3.internal.http2.StreamResetException
import retrofit2.HttpException
import java.io.FileNotFoundException
import java.io.InterruptedIOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import javax.inject.Inject

/**
 * Resolves commonly encountered errors and and constructs a user understandable
 * title and message that can be shown by the View.
 */
class ErrorResolver @Inject
constructor() {

  fun resolve(error: Throwable?): ResolvedError {
    if (error == null) {
      return ResolvedError.create(
          ResolvedError.Type.UNKNOWN,
          R.string.common_unknown_error_emoji,
          R.string.common_unknown_error_message)
    }

    val actualError = findActualCause(error)

    return if (isNetworkTimeoutError(actualError)) {
      if (actualError is UnknownHostException
          && actualError.message != null
          && (actualError.message!!.contains("reddit.com") || actualError.message!!.contains("redditmedia.com"))) {
        ResolvedError.create(
            ResolvedError.Type.NETWORK_ERROR,
            R.string.common_network_error_emoji,
            R.string.common_network_error_with_reddit_message)

      } else {
        ResolvedError.create(
            ResolvedError.Type.NETWORK_ERROR,
            R.string.common_network_error_emoji,
            R.string.common_network_error_with_other_websites_message)
      }

    } else if (actualError is HttpException && actualError.code() == 503) {
      ResolvedError.create(
          ResolvedError.Type.REDDIT_IS_DOWN,
          R.string.common_reddit_is_down_error_emoji,
          R.string.common_reddit_is_down_error_message)

    } else if (actualError is ImgurApiRequestRateLimitReachedException) {
      ResolvedError.create(
          ResolvedError.Type.IMGUR_RATE_LIMIT_REACHED,
          R.string.common_imgur_rate_limit_error_emoji,
          R.string.common_imgur_request_rate_limit_error_message)

    } else if (actualError is ImgurApiUploadRateLimitReachedException) {
      ResolvedError.create(
          ResolvedError.Type.IMGUR_RATE_LIMIT_REACHED,
          R.string.common_imgur_rate_limit_error_emoji,
          R.string.common_imgur_upload_rate_limit_error_message)

    } else if (actualError is CancellationException || actualError is InterruptedIOException || actualError is InterruptedException) {
      ResolvedError.create(
          ResolvedError.Type.CANCELATION,
          R.string.common_error_cancelation_emoji,
          R.string.common_error_cancelation_message)

    } else if (actualError is IllegalStateException && actualError.message?.contains("no active user context") == true) {
      ResolvedError.create(
          ResolvedError.Type.KNOWN_BUT_IGNORED,
          R.string.common_error_known_jraw_emoji,
          R.string.common_error_known_jraw_message)

    } else if (actualError is FileNotFoundException && actualError.message?.contains("No content provider") == true) {
      ResolvedError.create(
          ResolvedError.Type.KNOWN_BUT_IGNORED,
          R.string.common_error_cancelation_emoji,
          R.string.common_error_cancelation_message)

    } else {
      ResolvedError.create(
          ResolvedError.Type.UNKNOWN,
          R.string.common_unknown_error_emoji,
          R.string.common_unknown_error_message)
    }
  }

  fun findActualCause(error: Throwable): Throwable {
    var actualError = error

    if (actualError is ExecutionException) {
      // AFAIK, thrown by Glide in situations like socket-timeout.
      actualError = findActualCause(actualError.cause!!)
    }
    if (actualError is GlideException && actualError.rootCauses.isNotEmpty()) {
      actualError = actualError.rootCauses.last()
    }
    if (actualError is CompositeException) {
      actualError = findActualCause(actualError.cause)
    }
    if (actualError is UndeliverableException) {
      actualError = findActualCause(actualError.cause!!)
    }
    if (actualError is RuntimeException && actualError.cause != null && isNetworkTimeoutError(actualError.cause)) {
      // Stupid JRAW wraps all HTTP exceptions with RuntimeException.
      // Update: this may no longer be true with JRAW v1.0 (Dank v.0.6.1).
      actualError = findActualCause(actualError.cause!!)
    }
    if (actualError is IllegalStateException && actualError.message?.contains("Reached retry limit") == true) {
      actualError = findActualCause(actualError.cause!!)
    }
    return actualError
  }

  private fun isNetworkTimeoutError(error: Throwable?): Boolean {
    return (error is SocketException
        || error is SocketTimeoutException
        || error is UnknownHostException
        || error is StreamResetException
        || error is ConnectionShutdownException)
  }
}
