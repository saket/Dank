package me.saket.dank.ui.appshortcuts;

import java.util.List;

import me.saket.dank.utils.SimpleDiffUtilsCallbacks;

public class AppShortcutsItemDiffer extends SimpleDiffUtilsCallbacks<AppShortcut> {

  public static AppShortcutsItemDiffer create(List<AppShortcut> oldShortcuts, List<AppShortcut> newShortcuts) {
    return new AppShortcutsItemDiffer(oldShortcuts, newShortcuts);
  }

  private AppShortcutsItemDiffer(List<AppShortcut> oldShortcuts, List<AppShortcut> newShortcuts) {
    super(oldShortcuts, newShortcuts);
  }

  @Override
  public boolean areItemsTheSame(AppShortcut oldShortcut, AppShortcut newShortcut) {
    return oldShortcut.label().equals(newShortcut.label());
  }

  @Override
  protected boolean areContentsTheSame(AppShortcut oldShortcut, AppShortcut newShortcut) {
    return oldShortcut.equals(newShortcut);
  }
}
