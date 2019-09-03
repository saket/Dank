package me.saket.dank.ui.preferences;

import android.content.Context;

import com.f2prateek.rx.preferences2.Preference;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.Lazy;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.notifs.CheckUnreadMessagesJobService;
import me.saket.dank.utils.NestedOptionsPopupMenu;
import me.saket.dank.utils.NestedOptionsPopupMenu.MenuStructure.SingleLineItem;
import me.saket.dank.utils.NestedOptionsPopupMenu.MenuStructure.ThreeLineItem;
import me.saket.dank.utils.TimeInterval;

public class MessageCheckFrequencyPreferencePopup extends NestedOptionsPopupMenu {

  private static final int ID_DISABLE = 0;

  private static final int ID_15_MINS = 20;
  private static final int ID_30_MINS = 21;
  private static final int ID_1_HOUR = 22;
  private static final int ID_3_HOURS = 23;
  private static final int ID_6_HOURS = 24;
  private static final int ID_24_HOURS = 25;

  private static final int ID_NETWORK_WIFI_ONLY = 30;
  private static final int ID_NETWORK_WIFI_OR_MOBILE = 31;

  @Inject @Named("unread_messages") Lazy<Preference<TimeInterval>> frequencyPref;
  @Inject @Named("unread_messages") Lazy<Preference<NetworkStrategy>> networkStrategyPref;
  @Inject @Named("unread_messages") Lazy<Preference<Boolean>> enabledPref;

  private boolean preferencesChanged;

  public MessageCheckFrequencyPreferencePopup(Context c) {
    super(c);
    Dank.dependencyInjector().inject(this);

    createMenuLayout(c, menuStructure(c));

    setOnDismissListener(() -> {
      if (preferencesChanged) {
        if (enabledPref.get().get()) {
          CheckUnreadMessagesJobService.schedule(c, frequencyPref.get(), networkStrategyPref.get());
        } else {
          CheckUnreadMessagesJobService.unSchedule(c);
        }
      }
    });
  }

  private MenuStructure menuStructure(Context c) {
    List<SingleLineItem> firstPageItems = new ArrayList<>(3);

    List<ThreeLineItem> intervalItems = new ArrayList<>();
    intervalItems.add(ThreeLineItem.create(ID_15_MINS, c.getString(R.string.userprefs_checkfornewmessages_15_mins)));
    intervalItems.add(ThreeLineItem.create(ID_30_MINS, c.getString(R.string.userprefs_checkfornewmessages_30_mins)));
    intervalItems.add(ThreeLineItem.create(ID_1_HOUR, c.getString(R.string.userprefs_checkfornewmessages_1_hour)));
    intervalItems.add(ThreeLineItem.create(ID_3_HOURS, c.getString(R.string.userprefs_checkfornewmessages_3_hours)));
    intervalItems.add(ThreeLineItem.create(ID_6_HOURS, c.getString(R.string.userprefs_checkfornewmessages_6_hours)));
    intervalItems.add(ThreeLineItem.create(ID_24_HOURS, c.getString(R.string.userprefs_checkfornewmessages_24_hours)));
    firstPageItems.add(SingleLineItem.create(
        c.getString(R.string.userprefs_checkfornewmessages_poll_interval),
        R.drawable.ic_time_20dp,
        intervalItems));

    List<ThreeLineItem> networkTypeItems = new ArrayList<>(3);
    networkTypeItems.add(ThreeLineItem.create(ID_NETWORK_WIFI_ONLY, c.getString(NetworkStrategy.WIFI_ONLY.displayNameRes)));
    networkTypeItems.add(ThreeLineItem.create(ID_NETWORK_WIFI_OR_MOBILE, c.getString(NetworkStrategy.WIFI_OR_MOBILE_DATA.displayNameRes)));
    firstPageItems.add(SingleLineItem.create(
        c.getString(R.string.userprefs_checkfornewmessages_network_type),
        R.drawable.ic_network_wifi_20dp,
        networkTypeItems));

    firstPageItems.add(SingleLineItem.create(
        ID_DISABLE,
        c.getString(R.string.userprefs_checkfornewmessages_disable),
        R.drawable.ic_sync_disabled_20dp));

    return MenuStructure.create(c.getString(R.string.userprefs_check_for_new_messages), firstPageItems);
  }

  @Override
  protected void handleAction(Context c, int actionId) {
    preferencesChanged = true;

    switch (actionId) {
      case ID_DISABLE:
        break;

      case ID_15_MINS:
        frequencyPref.get().set(TimeInterval.create(15, TimeUnit.MINUTES));
        break;

      case ID_30_MINS:
        frequencyPref.get().set(TimeInterval.create(30, TimeUnit.MINUTES));
        break;

      case ID_1_HOUR:
        frequencyPref.get().set(TimeInterval.create(1, TimeUnit.HOURS));
        break;

      case ID_3_HOURS:
        frequencyPref.get().set(TimeInterval.create(3, TimeUnit.HOURS));
        break;

      case ID_6_HOURS:
        frequencyPref.get().set(TimeInterval.create(6, TimeUnit.HOURS));
        break;

      case ID_24_HOURS:
        frequencyPref.get().set(TimeInterval.create(1, TimeUnit.DAYS));
        break;

      case ID_NETWORK_WIFI_ONLY:
        networkStrategyPref.get().set(NetworkStrategy.WIFI_ONLY);
        break;

      case ID_NETWORK_WIFI_OR_MOBILE:
        networkStrategyPref.get().set(NetworkStrategy.WIFI_OR_MOBILE_DATA);
        break;

      default:
        throw new UnsupportedOperationException("Unknown actionId: " + actionId);
    }

    enabledPref.get().set(actionId != ID_DISABLE);

    if (actionId == ID_DISABLE) {
      dismiss();
    } else {
      gotoPrimaryPage();
    }
  }
}
