package me.saket.dank.ui.preferences.adapter;

import java.util.List;

import me.saket.dank.ui.subreddit.SimpleDiffUtilsCallbacks;

public class UserPrefsItemDiffer extends SimpleDiffUtilsCallbacks<UserPreferencesScreenUiModel> {

  public static UserPrefsItemDiffer create(List<UserPreferencesScreenUiModel> oldModels, List<UserPreferencesScreenUiModel> newModels) {
    return new UserPrefsItemDiffer(oldModels, newModels);
  }

  private UserPrefsItemDiffer(List<UserPreferencesScreenUiModel> oldModels, List<UserPreferencesScreenUiModel> newModels) {
    super(oldModels, newModels);
  }

  @Override
  public boolean areItemsTheSame(UserPreferencesScreenUiModel oldModel, UserPreferencesScreenUiModel newModel) {
    return oldModel.adapterId() == newModel.adapterId();
  }

  @Override
  protected boolean areContentsTheSame(UserPreferencesScreenUiModel oldModel, UserPreferencesScreenUiModel newModel) {
    return oldModel.equals(newModel);
  }
}
