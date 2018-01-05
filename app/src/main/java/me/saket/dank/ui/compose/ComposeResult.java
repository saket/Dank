package me.saket.dank.ui.compose;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import me.saket.dank.data.PostedOrInFlightContribution;
import me.saket.dank.utils.Optional;

@AutoValue
public abstract class ComposeResult implements Parcelable {

  /** Empty if {@link ComposeStartOptions#optionalParentContribution()} was also empty. */
  public Optional<PostedOrInFlightContribution> optionalParentContribution() {
    return Optional.ofNullable(parentContribution());
  }

  @Nullable
  abstract PostedOrInFlightContribution parentContribution();

  public abstract CharSequence reply();

  /**
   * Payload that was originally sent with {@link ComposeStartOptions}.
   */
  @Nullable
  public abstract Bundle extras();

  public static ComposeResult create(Optional<PostedOrInFlightContribution> optionalParentContribution, CharSequence reply, @Nullable Bundle extras) {
    PostedOrInFlightContribution parentContribution = optionalParentContribution.isPresent() ? optionalParentContribution.get() : null;
    return new AutoValue_ComposeResult(parentContribution, reply, extras);
  }
}
