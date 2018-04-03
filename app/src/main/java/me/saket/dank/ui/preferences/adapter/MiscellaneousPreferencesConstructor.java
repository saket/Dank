package me.saket.dank.ui.preferences.adapter;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import me.saket.dank.R;
import me.saket.dank.ui.preferences.adapter.UserPreferenceButton.UiModel;

public class MiscellaneousPreferencesConstructor implements UserPreferencesConstructor.ChildConstructor {

  @Inject
  public MiscellaneousPreferencesConstructor() {
  }

  @Override
  public List<UserPreferencesScreenUiModel> construct(Context c) {
    // - Bypass domains for external apps
    // - Default browser
    // - Launcher shortcuts
    List<UserPreferencesScreenUiModel> uiModels = new ArrayList<>();

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(c.getString(R.string.userprefs_group_external_links)));

    uiModels.add(UiModel.create(
        c.getString(R.string.userprefs_externallinks_default_web_browser),
        c.getString(R.string.userprefs_externallinks_internal_browser),
        (screen1, event1) -> {
        }));

    // TODO: This should probably be enabled only if Dank's internal web-browser is enabled.
    // TODO: Improve this text. Copy CatchUp's text.
    uiModels.add(UiModel.create(
        c.getString(R.string.userprefs_externallinks_open_links_in_apps),
        c.getString(R.string.userprefs_externallinks_open_links_in_apps_summary),
        (clickHandler, event) -> {
        }));

    return uiModels;
  }
}
