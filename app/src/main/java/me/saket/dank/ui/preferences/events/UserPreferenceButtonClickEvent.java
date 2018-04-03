package me.saket.dank.ui.preferences.events;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class UserPreferenceButtonClickEvent {

  public abstract UserPreferenceClickListener clickListener();

  public abstract int itemPosition();

  public abstract long itemId();

  public static UserPreferenceButtonClickEvent create(UserPreferenceClickListener clickListener, int itemPosition, long itemId) {
    return new AutoValue_UserPreferenceButtonClickEvent(clickListener, itemPosition, itemId);
  }
}
