package me.saket.dank.ui.compose;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Contribution;

@AutoValue
public abstract class ComposeStartOptions implements Parcelable {

  @Nullable
  public abstract String secondPartyName();

  @Nullable
  public abstract CharSequence preFilledText();

  @Nullable
  public abstract String parentContributionFullName();

  public abstract String draftKey();

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

    public abstract Builder preFilledText(CharSequence preFilledText);

    /**
     * The {@link Contribution} to which this reply is being made.
     * Sent back to calling Activity through {@link ComposeResult}.
     * Not used for anything else.
     */
    public Builder parentContribution(Contribution replyingTo) {
      return parentContributionFullName(replyingTo.getFullName());
    }

    /**
     * The {@link Contribution} to which this reply is being made.
     * Sent back to calling Activity through {@link ComposeResult}.
     * Not used for anything else.
     */
    abstract Builder parentContributionFullName(String replyingTo);

    /**
     * The ID to use for saving and retaining drafts.
     */
    public Builder draftKey(Contribution contribution) {
      return draftKey(contribution.getFullName());
    }

    /**
     * The ID to use for saving and retaining drafts.
     */
    abstract Builder draftKey(String key);

    /**
     * Delivered back with {@link ComposeResult}.
     */
    public abstract Builder extras(Bundle extras);

    public abstract ComposeStartOptions build();
  }
}
