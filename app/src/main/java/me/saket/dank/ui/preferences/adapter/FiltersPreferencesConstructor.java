package me.saket.dank.ui.preferences.adapter;

import android.content.Context;

import com.f2prateek.rx.preferences2.Preference;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import me.saket.dank.R;

public class FiltersPreferencesConstructor implements UserPreferencesConstructor.ChildConstructor {

  private final Preference<Boolean> showNsfwContentPref;

  @Inject
  public FiltersPreferencesConstructor(
      @Named("show_nsfw_content") Preference<Boolean> showNsfwContentPref)
  {
    this.showNsfwContentPref = showNsfwContentPref;
  }

  public List<UserPreferencesScreenUiModel> construct(Context c) {
    List<UserPreferencesScreenUiModel> uiModels = new ArrayList<>();

    uiModels.add(new UserPreferenceSwitch.UiModel(
        c.getString(R.string.userprefs_show_nsfw_content),
        showNsfwContentPref.get()
            ? c.getString(R.string.userprefs_show_nsfw_content_summary_on)
            : c.getString(R.string.userprefs_show_nsfw_content_summary_off),
        showNsfwContentPref.get(),
        showNsfwContentPref
    ));
    return uiModels;
  }
}
