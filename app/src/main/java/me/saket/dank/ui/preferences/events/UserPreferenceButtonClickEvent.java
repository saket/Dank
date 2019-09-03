package me.saket.dank.ui.preferences.events;

import com.google.auto.value.AutoValue;

import me.saket.dank.ui.preferences.adapter.UserPreferenceButton.ViewHolder;

@AutoValue
public abstract class UserPreferenceButtonClickEvent {

  public abstract UserPreferenceClickListener clickListener();

  public abstract ViewHolder itemViewHolder();

  public static UserPreferenceButtonClickEvent create(UserPreferenceClickListener clickListener, ViewHolder holder) {
    return new AutoValue_UserPreferenceButtonClickEvent(clickListener, holder);
  }
}
