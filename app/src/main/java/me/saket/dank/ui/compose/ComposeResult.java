package me.saket.dank.ui.compose;

import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Identifiable;

import me.saket.dank.utils.Optional;

@AutoValue
public abstract class ComposeResult implements Parcelable {

  /**
   * Empty if {@link ComposeStartOptions#optionalParent()} was also empty.
   */
  public Optional<Identifiable> optionalParentContribution() {
    return Optional.ofNullable(parent());
  }

  @Nullable
  abstract SimpleIdentifiable parent();

  public abstract CharSequence reply();

  /**
   * Payload that was originally sent with {@link ComposeStartOptions}.
   */
  @Nullable
  public abstract Bundle extras();

  public static ComposeResult create(Optional<Identifiable> optionalParent, CharSequence reply, @Nullable Bundle extras) {
    SimpleIdentifiable parent = optionalParent.map(SimpleIdentifiable.Companion::from).orElse(null);
    return new AutoValue_ComposeResult(parent, reply, extras);
  }
}
