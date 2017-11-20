package me.saket.dank.ui.compose;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Contribution;

@AutoValue
public abstract class ComposeStartOptions implements Parcelable {

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

    public abstract Builder secondPartyName(String secondPartyName);

    public abstract Builder preFilledText(String preFilledText);

    /**
     * See {@link ComposeStartOptions#parentContributionFullName()}.
     */
    public abstract Builder parentContributionFullName(String replyingTo);

    public abstract ComposeStartOptions build();
  }
}
