package me.saket.dank.ui.appshortcuts;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class AppShortcutPlaceholderUiModel implements AppShortcutScreenUiModel {

  @Override
  public long adapterId() {
    return AppShortcutsAdapter.ID_ADD_NEW;
  }

  public static AppShortcutPlaceholderUiModel create() {
    return new AutoValue_AppShortcutPlaceholderUiModel();
  }
}
