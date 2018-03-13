package me.saket.dank.ui.preferences.events;

import android.support.annotation.LayoutRes;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class UserPreferenceButtonClickEvent {

  @LayoutRes
  public abstract int preferenceScreenLayoutRes();

  public abstract int itemPosition();

  public abstract long itemId();

  public static UserPreferenceButtonClickEvent create(@LayoutRes int preferenceScreenLayoutRes, int itemPosition, long itemId) {
    return new AutoValue_UserPreferenceButtonClickEvent(preferenceScreenLayoutRes, itemPosition, itemId);
  }
}
