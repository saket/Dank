package me.saket.dank.data;

import androidx.annotation.StringRes;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ErrorState {

  @StringRes
  public abstract int emojiRes();

  @StringRes
  public abstract int messageRes();

  public static ErrorState create(@StringRes int emojiRes, @StringRes int messageRes) {
    return new AutoValue_ErrorState(emojiRes, messageRes);
  }

  public static ErrorState from(ResolvedError resolvedError) {
    return create(resolvedError.errorEmojiRes(), resolvedError.errorMessageRes());
  }
}
