package me.saket.dank.ui.compose;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ComposeResult implements Parcelable {

  public abstract String parentContributionFullName();

  public abstract String reply();

  /**
   * Payload that was originally sent with {@link ComposeStartOptions}.
   */
  @Nullable
  public abstract Bundle extras();

  public static ComposeResult create(String parentContributionFullName, String reply, @Nullable Bundle extras) {
    return new AutoValue_ComposeResult(parentContributionFullName, reply, extras);
  }
}
