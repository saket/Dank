package me.saket.dank.ui.appshortcuts;

import java.util.List;

import me.saket.dank.utils.SimpleDiffUtilsCallbacks;

public class AppShortcutsUiModelDiffer extends SimpleDiffUtilsCallbacks<AppShortcutScreenUiModel> {

  public static AppShortcutsUiModelDiffer create(List<AppShortcutScreenUiModel> oldModels, List<AppShortcutScreenUiModel> newModels) {
    return new AppShortcutsUiModelDiffer(oldModels, newModels);
  }

  private AppShortcutsUiModelDiffer(List<AppShortcutScreenUiModel> oldModels, List<AppShortcutScreenUiModel> newModels) {
    super(oldModels, newModels);
  }

  @Override
  public boolean areItemsTheSame(AppShortcutScreenUiModel oldModel, AppShortcutScreenUiModel newModel) {
    return oldModel.adapterId() == newModel.adapterId();
  }

  @Override
  protected boolean areContentsTheSame(AppShortcutScreenUiModel oldModel, AppShortcutScreenUiModel newModel) {
    return oldModel.equals(newModel);
  }
}
