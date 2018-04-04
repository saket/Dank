package me.saket.dank.ui.preferences.adapter;

import android.content.Context;
import com.f2prateek.rx.preferences2.Preference;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import me.saket.dank.R;
import me.saket.dank.data.NetworkStrategy;
import me.saket.dank.ui.preferences.PreferenceMultiOptionPopup;
import me.saket.dank.ui.preferences.adapter.UserPreferenceButton.UiModel;

public class DataUsagePreferencesConstructor implements UserPreferencesConstructor.ChildConstructor {

  private final Preference<NetworkStrategy> hqImagesNetworkStrategyPref;

  @Inject
  public DataUsagePreferencesConstructor(
      @Named("hq_images") Preference<NetworkStrategy> hqImagesNetworkStrategyPref)
  {
    this.hqImagesNetworkStrategyPref = hqImagesNetworkStrategyPref;
  }

  @Override
  public List<UserPreferencesScreenUiModel> construct(Context c) {
    List<UserPreferencesScreenUiModel> uiModels = new ArrayList<>();

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(c.getString(R.string.userprefs_group_messaging)));

    uiModels.add(UiModel.create(
        c.getString(R.string.userprefs_datausage_check_for_new_messages),
        c.getString(R.string.userprefs_datausage_message_sync_period_and_connection),
        (clickHandler, event) -> {
        }));

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(c.getString(R.string.userprefs_group_media_quality)));

    uiModels.add(UiModel.create(
        c.getString(R.string.userprefs_mediaquality_load_hq_images),
        c.getString(hqImagesNetworkStrategyPref.get().displayNameRes),
        (clickHandler, event) -> {
          PreferenceMultiOptionPopup.Builder<NetworkStrategy> popupBuilder = PreferenceMultiOptionPopup.builder(hqImagesNetworkStrategyPref)
              .addOption(NetworkStrategy.WIFI_ONLY, NetworkStrategy.WIFI_ONLY.displayNameRes)
              .addOption(NetworkStrategy.WIFI_OR_MOBILE_DATA, NetworkStrategy.WIFI_OR_MOBILE_DATA.displayNameRes)
              .addOption(NetworkStrategy.NEVER, NetworkStrategy.NEVER.displayNameRes);
          clickHandler.show(popupBuilder, event.itemViewHolder());
        }));

    uiModels.add(UiModel.create(
        c.getString(R.string.userprefs_mediaquality_load_hq_videos),
        c.getString(R.string.userprefs_networkstrategy_only_on_wifi),
        (clickHandler, event) -> {
        }));

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(
        c.getString(R.string.userprefs_group_caching),
        c.getString(R.string.userprefs_group_caching_summary)));

    uiModels.add(UiModel.create(
        c.getString(R.string.userprefs_prefetch_comments),
        c.getString(R.string.userprefs_networkstrategy_only_on_wifi),
        (clickHandler, event) -> {
        }));

    uiModels.add(UiModel.create(
        c.getString(R.string.userprefs_prefetch_link_descriptions),
        c.getString(R.string.userprefs_networkstrategy_only_on_wifi),
        (clickHandler, event) -> {
        }));

    uiModels.add(UiModel.create(
        c.getString(R.string.userprefs_prefetch_images),
        c.getString(R.string.userprefs_networkstrategy_only_on_wifi),
        (clickHandler, event) -> {
        }));

    return uiModels;
  }
}
