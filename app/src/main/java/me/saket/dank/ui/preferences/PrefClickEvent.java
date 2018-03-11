package me.saket.dank.ui.preferences;

import android.view.View;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PrefClickEvent {
  public abstract int itemPosition();
  public abstract long itemId();

  public static PrefClickEvent create(int itemPosition, long itemId) {
    return new AutoValue_PrefClickEvent(itemPosition, itemId);
  }
}
