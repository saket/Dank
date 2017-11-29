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

  public abstract String parentContributionFullName();

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
     * Used as an ID for saving and retaining drafts.
     */
    public Builder parentContribution(Contribution replyingTo) {
      return parentContributionFullName(replyingTo.getFullName());
    }

    /**
     * The {@link Contribution}'s fullname to which this reply is being made.
     * Used as an ID for saving and retaining drafts.
     */
    abstract Builder parentContributionFullName(String replyingTo);

    /**
     * Delivered back with {@link ComposeResult}.
     */
    public abstract Builder extras(Bundle extras);

    public abstract ComposeStartOptions build();
  }
}
