package me.saket.dank.ui.compose;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Contribution;

import me.saket.dank.data.ContributionFullNameWrapper;
import me.saket.dank.utils.Optional;

@AutoValue
public abstract class ComposeStartOptions implements Parcelable {

  @Nullable
  public abstract String secondPartyName();

  @Nullable
  abstract ContributionFullNameWrapper parentContribution();

  public Optional<ContributionFullNameWrapper> optionalParentContribution() {
    return Optional.ofNullable(parentContribution());
  }

  public abstract ContributionFullNameWrapper draftKey();

  @Nullable
  public abstract Bundle extras();

  public static ComposeStartOptions.Builder builder() {
    return new AutoValue_ComposeStartOptions.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    /**
     * When null, {@link ComposeReplyActivity} just uses "Reply".
     */
    public abstract Builder secondPartyName(@Nullable String secondPartyName);

    /**
     * The {@link Contribution} to which this reply is being made.
     * Sent back to calling Activity through {@link ComposeResult}.
     * Not used for anything else.
     * <p>
     * Optional only if you don't want it back in {@link ComposeResult}.
     */
    public Builder parentContribution(Optional<ContributionFullNameWrapper> replyingTo) {
      return parentContribution(replyingTo.orElse(null));
    }

    /**
     * The {@link Contribution} to which this reply is being made.
     * Sent back to calling Activity through {@link ComposeResult}.
     * Not used for anything else.
     */
    public abstract Builder parentContribution(@Nullable ContributionFullNameWrapper replyingTo);

    /**
     * The ID (contribution's full-name) to use for saving and retaining drafts.
     */
    public abstract Builder draftKey(ContributionFullNameWrapper contributionAsDraftKey);

    /**
     * Delivered back with {@link ComposeResult}.
     */
    public abstract Builder extras(Bundle extras);

    public abstract ComposeStartOptions build();
  }
}
