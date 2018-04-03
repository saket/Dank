package me.saket.dank.ui.preferences.events;

import me.saket.dank.ui.preferences.PreferenceButtonClickHandler;

public interface UserPreferenceClickListener {

  void onClick(PreferenceButtonClickHandler clickHandler, UserPreferenceButtonClickEvent event);
}
