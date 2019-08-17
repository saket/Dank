package me.saket.dank.data;

import androidx.annotation.StringRes;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class EmptyState {

  @StringRes
  public abstract int emojiRes();

  @StringRes
  public abstract int messageRes();

  public static EmptyState create(@StringRes int emojisRes, @StringRes int messageRes) {
    return new AutoValue_EmptyState(emojisRes, messageRes);
  }
}
