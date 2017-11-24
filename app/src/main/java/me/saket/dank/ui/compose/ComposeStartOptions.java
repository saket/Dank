package me.saket.dank.ui.compose;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Contribution;

// TODO: NOT PARCELABLE!
@AutoValue
public abstract class ComposeStartOptions implements Parcelable {

  @Nullable
  public abstract String secondPartyName();

  @Nullable
  public abstract String preFilledText();

  /**
   * The {@link Contribution} to which this reply is being made. Used as an ID for saving drafts.
   */
  public abstract String parentContributionFullName();

  public abstract ComposeStartOptions.Builder toBuilder();

  public static ComposeStartOptions.Builder builder() {
    return new AutoValue_ComposeStartOptions.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    /**
     * When null, {@link ComposeReplyActivity} just uses "Reply".
     */
    public abstract Builder secondPartyName(@Nullable String secondPartyName);

    public abstract Builder preFilledText(String preFilledText);

    /**
     * See {@link ComposeStartOptions#parentContributionFullName()}.
     */
    public Builder parentContribution(Contribution replyingTo) {
      return parentContributionFullName(replyingTo.getFullName());
    }

    abstract Builder parentContributionFullName(String replyingTo);

    public abstract ComposeStartOptions build();
  }
}
