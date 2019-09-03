package me.saket.dank.ui.preferences.adapter;

import android.content.Context;
import android.graphics.Point;
import android.view.Gravity;

import com.f2prateek.rx.preferences2.Preference;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.Lazy;
import me.saket.dank.R;
import me.saket.dank.ui.preferences.MessageCheckFrequencyPreferencePopup;
import me.saket.dank.ui.preferences.MultiOptionPreferencePopup;
import me.saket.dank.ui.preferences.NetworkStrategy;
import me.saket.dank.ui.preferences.adapter.UserPreferenceButton.UiModel;
import me.saket.dank.utils.TimeInterval;
import me.saket.dank.utils.Views;

public class DataUsagePreferencesConstructor implements UserPreferencesConstructor.ChildConstructor {

  private final Lazy<Preference<TimeInterval>> messagesPollFrequencyPref;
  private final Lazy<Preference<NetworkStrategy>> messagesPollNetworkStrategyPref;
  private final Lazy<Preference<Boolean>> messagesPollEnabledPref;
  private final Preference<NetworkStrategy> hdMediaInSubmissionsNetworkStrategyPref;
  private final Preference<NetworkStrategy> hdMediaInGalleryNetworkStrategyPref;
  private final Preference<NetworkStrategy> autoPlayVideosNetworkStrategyPref;
  private final Preference<NetworkStrategy> commentsPreFetchNetworkStrategyPref;
  private final Preference<NetworkStrategy> linksPreFetchNetworkStrategyPref;
  private final Preference<NetworkStrategy> imagesPreFetchNetworkStrategyPref;

  @Inject
  public DataUsagePreferencesConstructor(
      @Named("unread_messages") Lazy<Preference<TimeInterval>> messagesPollFrequencyPref,
      @Named("unread_messages") Lazy<Preference<NetworkStrategy>> messagesPollNetworkStrategyPref,
      @Named("unread_messages") Lazy<Preference<Boolean>> messagesPollEnabledPref,
      @Named("hd_media_in_submissions") Preference<NetworkStrategy> hdMediaInSubmissionsNetworkStrategyPref,
      @Named("hd_media_in_gallery") Preference<NetworkStrategy> hdMediaInGalleryNetworkStrategyPref,
      @Named("auto_play_videos") Preference<NetworkStrategy> autoPlayVideosNetworkStrategyPref,
      @Named("comments_prefetch") Preference<NetworkStrategy> commentsPreFetchNetworkStrategyPref,
      @Named("links_prefetch") Preference<NetworkStrategy> linksPreFetchNetworkStrategyPref,
      @Named("images_prefetch") Preference<NetworkStrategy> imagesPreFetchNetworkStrategyPref)
  {
    this.messagesPollFrequencyPref = messagesPollFrequencyPref;
    this.messagesPollNetworkStrategyPref = messagesPollNetworkStrategyPref;
    this.messagesPollEnabledPref = messagesPollEnabledPref;
    this.hdMediaInSubmissionsNetworkStrategyPref = hdMediaInSubmissionsNetworkStrategyPref;
    this.hdMediaInGalleryNetworkStrategyPref = hdMediaInGalleryNetworkStrategyPref;
    this.autoPlayVideosNetworkStrategyPref = autoPlayVideosNetworkStrategyPref;
    this.commentsPreFetchNetworkStrategyPref = commentsPreFetchNetworkStrategyPref;
    this.linksPreFetchNetworkStrategyPref = linksPreFetchNetworkStrategyPref;
    this.imagesPreFetchNetworkStrategyPref = imagesPreFetchNetworkStrategyPref;
  }

  @Override
  public List<UserPreferencesScreenUiModel> construct(Context c) {
    List<UserPreferencesScreenUiModel> uiModels = new ArrayList<>();

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(c.getString(R.string.userprefs_group_messaging)));

    uiModels.add(UiModel.create(
        c.getString(R.string.userprefs_check_for_new_messages),
        messagePollIntervalAndNetworkTypeSummary(c),
        (clickHandler, event) -> {
          Point showLocation = new Point(0, event.itemViewHolder().itemView.getTop() + Views.statusBarHeight(c.getResources()));

          // Align with padding.
          int padding = c.getResources().getDimensionPixelSize(R.dimen.userprefs_item_padding_for_preference_popups);
          showLocation.offset(padding, padding);

          MessageCheckFrequencyPreferencePopup popup = new MessageCheckFrequencyPreferencePopup(c);
          popup.showAtLocation(event.itemViewHolder().itemView, Gravity.NO_GRAVITY, showLocation);
        }));

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(c.getString(R.string.userprefs_group_media_quality)));

    uiModels.add(UiModel.create(
        c.getString(R.string.userprefs_mediaquality_load_hq_media_in_submissions),
        c.getString(hdMediaInSubmissionsNetworkStrategyPref.get().displayNameRes),
        (clickHandler, event) -> clickHandler.show(networkStrategyPopup(hdMediaInSubmissionsNetworkStrategyPref), event.itemViewHolder())));

    uiModels.add(UiModel.create(
        c.getString(R.string.userprefs_mediaquality_load_hq_media_in_gallery),
        c.getString(hdMediaInGalleryNetworkStrategyPref.get().displayNameRes),
        (clickHandler, event) -> clickHandler.show(networkStrategyPopup(hdMediaInGalleryNetworkStrategyPref), event.itemViewHolder())));

    uiModels.add(UiModel.create(
        c.getString(R.string.userprefs_mediaquality_autoplay_videos),
        c.getString(autoPlayVideosNetworkStrategyPref.get().displayNameRes),
        (clickHandler, event) -> clickHandler.show(networkStrategyPopup(autoPlayVideosNetworkStrategyPref), event.itemViewHolder())));

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

  private String messagePollIntervalAndNetworkTypeSummary(Context c) {
    Boolean enabled = messagesPollEnabledPref.get().get();
    if (!enabled) {
      return c.getString(R.string.userprefs_messages_poll_interval_never);
    }

    TimeInterval interval = messagesPollFrequencyPref.get().get();
    NetworkStrategy strategy = messagesPollNetworkStrategyPref.get().get();

    String intervalString;
    switch (interval.timeUnit()) {
      case DAYS:
        intervalString = c.getResources().getQuantityString(
            R.plurals.userprefs_message_poll_interval_days,
            ((int) interval.interval()),
            interval.interval());
        break;

      case HOURS:
        intervalString = c.getResources().getQuantityString(
            R.plurals.userprefs_message_poll_interval_hours,
            ((int) interval.interval()),
            interval.interval());
        break;

      case MINUTES:
        intervalString = c.getResources().getQuantityString(
            R.plurals.userprefs_message_poll_interval_minutes,
            ((int) interval.interval()),
            interval.interval());
        break;

      default:
        throw new AssertionError("Unexpected time interval: " + interval);
    }

    if (strategy == NetworkStrategy.WIFI_ONLY) {
      return c.getString(R.string.userprefs_messages_poll_interval_on_wifi, intervalString);

    } else if (strategy == NetworkStrategy.WIFI_OR_MOBILE_DATA) {
      return c.getString(R.string.userprefs_messages_poll_interval_on_wifi_or_mobile, intervalString);

    } else {
      throw new AssertionError("Unexpected network strategy: " + strategy);
    }
  }

  private MultiOptionPreferencePopup.Builder<NetworkStrategy> networkStrategyPopup(Preference<NetworkStrategy> preference) {
    return MultiOptionPreferencePopup.builder(preference)
        .addOption(NetworkStrategy.WIFI_ONLY, NetworkStrategy.WIFI_ONLY.displayNameRes, R.drawable.ic_network_wifi_20dp)
        .addOption(NetworkStrategy.WIFI_OR_MOBILE_DATA, NetworkStrategy.WIFI_OR_MOBILE_DATA.displayNameRes, R.drawable.ic_network_cell_20dp)
        .addOption(NetworkStrategy.NEVER, NetworkStrategy.NEVER.displayNameRes, R.drawable.ic_block_20dp);
  }
}
