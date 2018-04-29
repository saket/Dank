package me.saket.dank.data;

import android.support.annotation.Nullable;

import com.bumptech.glide.load.engine.GlideException;

import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.UndeliverableException;
import me.saket.dank.R;
import me.saket.dank.data.exceptions.ImgurApiRequestRateLimitReachedException;
import me.saket.dank.data.exceptions.ImgurApiUploadRateLimitReachedException;
import me.saket.dank.utils.Optional;
import retrofit2.HttpException;

/**
 * Resolves commonly encountered errors and and constructs a user understandable
 * title and message that can be shown by the View.
 */
public class ErrorResolver {

  @Inject
  public ErrorResolver() {
  }

  public ResolvedError resolve(@Nullable Throwable error) {
    error = findActualCause(error);

    if (isNetworkTimeoutError(error)) {
      if (error instanceof UnknownHostException
          && error.getMessage() != null
          && (error.getMessage().contains("reddit.com") || error.getMessage().contains("redditmedia.com")))
      {
        return ResolvedError.create(
            ResolvedError.Type.NETWORK_ERROR,
            R.string.common_network_error_emoji,
            R.string.common_network_error_with_reddit_message);

      } else {
        return ResolvedError.create(
            ResolvedError.Type.NETWORK_ERROR,
            R.string.common_network_error_emoji,
            R.string.common_network_error_with_other_websites_message);
      }

    } else if (error instanceof HttpException && ((HttpException) error).code() == 503) {
      return ResolvedError.create(
          ResolvedError.Type.REDDIT_IS_DOWN,
          R.string.common_reddit_is_down_error_emoji,
          R.string.common_reddit_is_down_error_message);

    } else if (error instanceof ImgurApiRequestRateLimitReachedException) {
      return ResolvedError.create(
          ResolvedError.Type.IMGUR_RATE_LIMIT_REACHED,
          R.string.common_imgur_rate_limit_error_emoji,
          R.string.common_imgur_request_rate_limit_error_message);

    } else if (error instanceof ImgurApiUploadRateLimitReachedException) {
      return ResolvedError.create(
          ResolvedError.Type.IMGUR_RATE_LIMIT_REACHED,
          R.string.common_imgur_rate_limit_error_emoji,
          R.string.common_imgur_upload_rate_limit_error_message);

    } else if (error instanceof CancellationException || error instanceof InterruptedIOException) {
      return ResolvedError.create(
          ResolvedError.Type.CANCELATION,
          R.string.common_error_cancelation_emoji,
          R.string.common_error_cancelation_message);

    } else if (error instanceof IllegalStateException
        && Optional.ofNullable(error.getMessage()).map(m -> m.contains("no active user context")).orElse(false))
    {
      return ResolvedError.create(
          ResolvedError.Type.UNKNOWN,
          R.string.common_error_known_jraw_emoji,
          R.string.common_error_known_jraw_message);

    } else {
      return ResolvedError.create(
          ResolvedError.Type.UNKNOWN,
          R.string.common_unknown_error_emoji,
          R.string.common_unknown_error_message);
    }
  }

  public Throwable findActualCause(@Nullable Throwable error) {
    if (error instanceof ExecutionException) {
      error = findActualCause(error.getCause());
    }
    if (error instanceof GlideException && !((GlideException) error).getRootCauses().isEmpty()) {
      error = ((GlideException) error).getRootCauses().get(0);
    }
    if (error instanceof CompositeException) {
      error = findActualCause(error.getCause());
    }
    if (error instanceof UndeliverableException) {
      error = findActualCause(error.getCause());
    }
    if (error instanceof RuntimeException && error.getCause() != null && isNetworkTimeoutError(error.getCause())) {
      // Stupid JRAW wraps all HTTP exceptions with RuntimeException.
      error = findActualCause(error.getCause());
    }
    if (error instanceof IllegalStateException && error.getMessage() != null && error.getMessage().contains("Reached retry limit")) {
      error = findActualCause(error.getCause());
    }
    return error;
  }

  private boolean isNetworkTimeoutError(@Nullable Throwable error) {
    return error instanceof SocketException || error instanceof SocketTimeoutException || error instanceof UnknownHostException;
  }
}
