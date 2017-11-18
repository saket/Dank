package me.saket.dank.data;

import android.support.annotation.StringRes;

import com.google.auto.value.AutoValue;

/**
 * Details of an error resolved by {@link ErrorResolver}.
 */
@AutoValue
public abstract class ResolvedError {

  public enum Type {
    UNKNOWN,
    NETWORK_ERROR,
    REDDIT_IS_DOWN,
    IMGUR_RATE_LIMIT_REACHED
  }

  public abstract Type type();

  @StringRes
  public abstract int errorEmojiRes();

  @StringRes
  public abstract int errorMessageRes();

  public boolean isUnknown() {
    return type() == Type.UNKNOWN;
  }

  public boolean isNetworkError() {
    return type() == Type.NETWORK_ERROR;
  }

  public boolean isRedditServerError() {
    return type() == Type.REDDIT_IS_DOWN;
  }

  public boolean isImgurRateLimitError() {
    return type() == Type.IMGUR_RATE_LIMIT_REACHED;
  }

  public static ResolvedError create(Type type, @StringRes int errorEmoji, @StringRes int errorMessage) {
    return new AutoValue_ResolvedError(type, errorEmoji, errorMessage);
  }
}
