package me.saket.dank.ui.preferences.events;

import com.f2prateek.rx.preferences2.Preference;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class UserPreferenceSwitchToggleEvent {

  public abstract Preference<Boolean> preference();

  public abstract boolean isChecked();

  public static UserPreferenceSwitchToggleEvent create(Preference<Boolean> preference, boolean isChecked) {
    return new AutoValue_UserPreferenceSwitchToggleEvent(preference, isChecked);
  }
}
