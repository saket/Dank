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

  private final Preference<NetworkStrategy> hqImagesLoadNetworkStrategyPref;
  private final Preference<NetworkStrategy> hqVideosLoadNetworkStrategyPref;
  private final Preference<NetworkStrategy> commentsPreFetchNetworkStrategyPref;
  private final Preference<NetworkStrategy> linksPreFetchNetworkStrategyPref;
  private final Preference<NetworkStrategy> imagesPreFetchNetworkStrategyPref;

  @Inject
  public DataUsagePreferencesConstructor(
      @Named("hq_images") Preference<NetworkStrategy> hqImagesNetworkStrategyPref,
      @Named("hq_videos") Preference<NetworkStrategy> hqVideosLoadNetworkStrategyPref,
      @Named("comments_prefetch") Preference<NetworkStrategy> commentsPreFetchNetworkStrategyPref,
      @Named("links_prefetch") Preference<NetworkStrategy> linksPreFetchNetworkStrategyPref,
      @Named("images_prefetch") Preference<NetworkStrategy> imagesPreFetchNetworkStrategyPref)
  {
    this.hqImagesLoadNetworkStrategyPref = hqImagesNetworkStrategyPref;
    this.hqVideosLoadNetworkStrategyPref = hqVideosLoadNetworkStrategyPref;
    this.commentsPreFetchNetworkStrategyPref = commentsPreFetchNetworkStrategyPref;
    this.linksPreFetchNetworkStrategyPref = linksPreFetchNetworkStrategyPref;
    this.imagesPreFetchNetworkStrategyPref = imagesPreFetchNetworkStrategyPref;
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
        c.getString(hqImagesLoadNetworkStrategyPref.get().displayNameRes),
        (clickHandler, event) -> clickHandler.show(networkStrategyPopup(hqImagesLoadNetworkStrategyPref), event.itemViewHolder())));

    uiModels.add(UiModel.create(
        c.getString(R.string.userprefs_mediaquality_load_hq_videos),
        c.getString(hqVideosLoadNetworkStrategyPref.get().displayNameRes),
        (clickHandler, event) -> clickHandler.show(networkStrategyPopup(hqVideosLoadNetworkStrategyPref), event.itemViewHolder())));

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(
        c.getString(R.string.userprefs_group_caching),
        c.getString(R.string.userprefs_group_caching_summary)));

    uiModels.add(UiModel.create(
        c.getString(R.string.userprefs_prefetch_comments),
        c.getString(commentsPreFetchNetworkStrategyPref.get().displayNameRes),
        (clickHandler, event) -> clickHandler.show(networkStrategyPopup(commentsPreFetchNetworkStrategyPref), event.itemViewHolder())));

    uiModels.add(UiModel.create(
        c.getString(R.string.userprefs_prefetch_link_descriptions),
        c.getString(linksPreFetchNetworkStrategyPref.get().displayNameRes),
       (clickHandler, event) -> clickHandler.show(networkStrategyPopup(linksPreFetchNetworkStrategyPref), event.itemViewHolder())));

    uiModels.add(UiModel.create(
        c.getString(R.string.userprefs_prefetch_images),
        c.getString(imagesPreFetchNetworkStrategyPref.get().displayNameRes),
       (clickHandler, event) -> clickHandler.show(networkStrategyPopup(imagesPreFetchNetworkStrategyPref), event.itemViewHolder())));

    return uiModels;
  }

  private PreferenceMultiOptionPopup.Builder<NetworkStrategy> networkStrategyPopup(Preference<NetworkStrategy> preference) {
    return PreferenceMultiOptionPopup.builder(preference)
        .addOption(NetworkStrategy.WIFI_ONLY, NetworkStrategy.WIFI_ONLY.displayNameRes, R.drawable.ic_network_wifi_24dp)
        .addOption(NetworkStrategy.WIFI_OR_MOBILE_DATA, NetworkStrategy.WIFI_OR_MOBILE_DATA.displayNameRes, R.drawable.ic_network_cell_24dp)
        .addOption(NetworkStrategy.NEVER, NetworkStrategy.NEVER.displayNameRes, R.drawable.ic_block_24dp);
  }
}
