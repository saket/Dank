package me.saket.dank.ui.compose;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ComposeResult implements Parcelable {

  @Nullable
  public abstract String parentContributionFullName();

  public abstract CharSequence reply();

  /**
   * Payload that was originally sent with {@link ComposeStartOptions}.
   */
  @Nullable
  public abstract Bundle extras();

  public static ComposeResult create(@Nullable String parentContributionFullName, CharSequence reply, @Nullable Bundle extras) {
    return new AutoValue_ComposeResult(parentContributionFullName, reply, extras);
  }
}
