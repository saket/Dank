package me.saket.dank.ui.preferences.adapter;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import me.saket.dank.R;

public class DataUsagePreferencesConstructor implements UserPreferencesConstructor.ChildConstructor {

  @Inject
  public DataUsagePreferencesConstructor() {
  }

  @Override
  public List<UserPreferencesScreenUiModel> construct(Context c) {
    // - Show high-res media by default: never, always on wi-fi (hidden).
    // - Message check frequency
    // - Pre-fetch:
    // - comments
    //     - link metadata
    //     - images
    List<UserPreferencesScreenUiModel> uiModels = new ArrayList<>();

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(c.getString(R.string.userprefs_group_messaging)));

    uiModels.add(UserPreferenceButton.UiModel.create(
        c.getString(R.string.userprefs_datausage_check_for_new_messages),
        c.getString(R.string.userprefs_datausage_message_sync_period_and_connection),
        -1));

    uiModels.add(UserPreferenceSectionHeader.UiModel.create("Media quality"));
    uiModels.add(UserPreferenceButton.UiModel.create("Load high-quality images", "Only on WiFi", -1));
    uiModels.add(UserPreferenceButton.UiModel.create("Load high-quality videos", "Only on WiFi", -1));

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(
        c.getString(R.string.userprefs_group_caching),
        c.getString(R.string.userprefs_group_caching_summary)));

    uiModels.add(UserPreferenceButton.UiModel.create(
        c.getString(R.string.userprefs_prefetch_comments),
        c.getString(R.string.userprefs_caching_only_on_wifi),
        -1));

    uiModels.add(UserPreferenceButton.UiModel.create(
        c.getString(R.string.userprefs_prefetch_link_descriptions),
        c.getString(R.string.userprefs_caching_only_on_wifi),
        -1));

    uiModels.add(UserPreferenceButton.UiModel.create(
        c.getString(R.string.userprefs_prefetch_images),
        c.getString(R.string.userprefs_caching_only_on_wifi),
        -1));

    return uiModels;
  }
}
