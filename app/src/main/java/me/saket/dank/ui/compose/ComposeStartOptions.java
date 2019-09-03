package me.saket.dank.ui.compose;

import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Identifiable;
import net.dean.jraw.models.PublicContribution;

import me.saket.dank.utils.Optional;

@AutoValue
public abstract class ComposeStartOptions implements Parcelable {

  @Nullable
  public abstract String secondPartyName();

  @Nullable
  abstract SimpleIdentifiable parent();

  public Optional<Identifiable> optionalParent() {
    return Optional.ofNullable(parent());
  }

  public abstract SimpleIdentifiable draftKey();

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
     * The {@link PublicContribution} to which this reply is being made.
     * Sent back to calling Activity through {@link ComposeResult}.
     * Not used for anything else.
     * <p>
     * Optional only if you don't want it back in {@link ComposeResult}.
     */
    public Builder parent(Optional<Identifiable> replingTo) {
      return parent(replingTo.map(SimpleIdentifiable.Companion::from).orElse(null));
    }

    /**
     * The {@link PublicContribution} to which this reply is being made.
     * Sent back to calling Activity through {@link ComposeResult}.
     * Not used for anything else.
     */
    public Builder parent(@Nullable Identifiable replyingTo) {
      return parent(replyingTo != null ? SimpleIdentifiable.Companion.from(replyingTo) : null);
    }

    abstract Builder parent(@Nullable SimpleIdentifiable replyingTo);

    /**
     * The ID (contribution's full-name) to use for saving and retaining drafts.
     */
    public Builder draftKey(Identifiable identifiableAsDraftKey) {
      return draftKey(SimpleIdentifiable.Companion.from(identifiableAsDraftKey));
    }

    abstract Builder draftKey(SimpleIdentifiable contributionAsDraftKey);

    /**
     * Delivered back with {@link ComposeResult}.
     */
    public abstract Builder extras(Bundle extras);

    public abstract ComposeStartOptions build();
  }
}
