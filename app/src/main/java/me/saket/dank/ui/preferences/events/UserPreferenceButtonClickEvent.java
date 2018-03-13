package me.saket.dank.ui.preferences.events;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class UserPreferenceButtonClickEvent {

  public abstract String preferenceKey();

  public abstract int itemPosition();

  public abstract long itemId();

  public static UserPreferenceButtonClickEvent create(String preferenceKey, int itemPosition, long itemId) {
    return new AutoValue_UserPreferenceButtonClickEvent(preferenceKey, itemPosition, itemId);
  }
}
