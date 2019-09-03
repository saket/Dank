package me.saket.dank.ui.preferences.adapter;

import android.content.Context;
import android.widget.Toast;

import com.f2prateek.rx.preferences2.Preference;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import me.saket.dank.R;
import me.saket.dank.ui.appshortcuts.ConfigureAppShortcutsActivity;
import me.saket.dank.ui.preferences.DefaultWebBrowser;
import me.saket.dank.ui.preferences.MultiOptionPreferencePopup;
import me.saket.dank.utils.DeviceInfo;

import static me.saket.dank.ui.preferences.DefaultWebBrowser.DANK_INTERNAL_BROWSER;

public class MiscellaneousPreferencesConstructor implements UserPreferencesConstructor.ChildConstructor {

  private final Preference<DefaultWebBrowser> defaultBrowserPref;
  private final DeviceInfo deviceInfo;

  @Inject
  public MiscellaneousPreferencesConstructor(Preference<DefaultWebBrowser> defaultBrowserPref, DeviceInfo deviceInfo) {
    this.defaultBrowserPref = defaultBrowserPref;
    this.deviceInfo = deviceInfo;
  }

  @Override
  public List<UserPreferencesScreenUiModel> construct(Context c) {
    // - Bypass domains for external apps
    // - Default browser
    // - Launcher shortcuts
    List<UserPreferencesScreenUiModel> uiModels = new ArrayList<>();

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(c.getString(R.string.userprefs_group_external_links)));

    uiModels.add(UserPreferenceButton.UiModel.create(
        c.getString(R.string.userprefs_externallinks_default_web_browser),
        c.getString(defaultBrowserPref.get().displayName),
        (clickHandler, event) -> clickHandler.show(defaultBrowserPopup(), event.itemViewHolder())));

    if (defaultBrowserPref.get() == DANK_INTERNAL_BROWSER) {
      uiModels.add(UserPreferenceButton.UiModel.create(
          c.getString(R.string.userprefs_externallinks_open_links_in_apps),
          c.getString(R.string.userprefs_externallinks_open_links_in_apps_summary),
          (clickHandler, event) -> Toast.makeText(c, R.string.work_in_progress, Toast.LENGTH_SHORT).show()));
    }

    if (deviceInfo.isNougatMrOneOrAbove()) {
      uiModels.add(UserPreferenceSectionHeader.UiModel.create(c.getString(R.string.userprefs_group_launcher)));

      uiModels.add(UserPreferenceButton.UiModel.create(
          c.getString(R.string.userprefs_launcher_app_shortcuts),
          c.getString(R.string.userprefs_launcher_app_shortcuts_summary),
          (clickHandler, event) -> clickHandler.openIntent(ConfigureAppShortcutsActivity.intent(c))));
    }

    return uiModels;
  }

  private MultiOptionPreferencePopup.Builder<DefaultWebBrowser> defaultBrowserPopup() {
    return MultiOptionPreferencePopup.builder(defaultBrowserPref)
        .addOption(DefaultWebBrowser.DANK_INTERNAL_BROWSER, DefaultWebBrowser.DANK_INTERNAL_BROWSER.displayName, R.drawable.ic_status_bar_20dp)
        .addOption(DefaultWebBrowser.CHROME_CUSTOM_TABS, DefaultWebBrowser.CHROME_CUSTOM_TABS.displayName, R.drawable.ic_google_chrome_20dp)
        .addOption(DefaultWebBrowser.DEVICE_DEFAULT, DefaultWebBrowser.DEVICE_DEFAULT.displayName, R.drawable.ic_smartphone_20dp);
  }
}
