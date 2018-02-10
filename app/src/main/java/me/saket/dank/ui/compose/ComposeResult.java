package me.saket.dank.ui.compose;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import me.saket.dank.data.ContributionFullNameWrapper;
import me.saket.dank.utils.Optional;

@AutoValue
public abstract class ComposeResult implements Parcelable {

  /**
   * Empty if {@link ComposeStartOptions#optionalParentContribution()} was also empty.
   */
  public Optional<ContributionFullNameWrapper> optionalParentContribution() {
    return Optional.ofNullable(parentContribution());
  }

  @Nullable
  abstract ContributionFullNameWrapper parentContribution();

  public abstract CharSequence reply();

  /**
   * Payload that was originally sent with {@link ComposeStartOptions}.
   */
  @Nullable
  public abstract Bundle extras();

  public static ComposeResult create(Optional<ContributionFullNameWrapper> optionalParentContribution, CharSequence reply, @Nullable Bundle extras) {
    ContributionFullNameWrapper parentContribution = optionalParentContribution.orElse(null);
    return new AutoValue_ComposeResult(parentContribution, reply, extras);
  }
}
