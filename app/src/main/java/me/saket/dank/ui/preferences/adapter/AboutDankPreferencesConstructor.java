package me.saket.dank.ui.preferences.adapter;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

public class AboutDankPreferencesConstructor implements UserPreferencesConstructor.ChildConstructor {

  @Inject
  public AboutDankPreferencesConstructor() {
  }

  @Override
  public List<UserPreferencesScreenUiModel> construct(Context c) {
    List<UserPreferencesScreenUiModel> uiModels = new ArrayList<>();
    return uiModels;
  }
}
