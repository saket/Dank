package me.saket.dank.di;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.squareup.moshi.Moshi;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Observable;
import me.saket.dank.data.CachePreFillThing;
import me.saket.dank.data.NetworkStrategy;
import me.saket.dank.utils.RxPreferencesEnumTypeAdapter;
import me.saket.dank.utils.TimeInterval;

@Module
public class UserPreferencesModule {

  @Provides
  @Singleton
  @Named("user_prefs")
  SharedPreferences provideSharedPrefsForUserPrefs(Application appContext) {
    return PreferenceManager.getDefaultSharedPreferences(appContext);
  }

  @Provides
  @Named("user_prefs")
  RxSharedPreferences provideRxSharedPrefs(@Named("user_prefs") SharedPreferences sharedPrefs) {
    return RxSharedPreferences.create(sharedPrefs);
  }

  @Provides
  @Named("comment_count_in_submission_list_byline")
  Preference<Boolean> showCommentCountInBylinePref(@Named("user_prefs") RxSharedPreferences rxPrefs) {
    return rxPrefs.getBoolean("comment_count_in_submission_list_byline", false);
  }

  @Provides
  @Named("show_nsfw_content")
  Preference<Boolean> showNsfwContentPref(@Named("user_prefs") RxSharedPreferences rxPrefs) {
    return rxPrefs.getBoolean("show_nsfw_content", false);
  }

  @Provides
  RxPreferencesEnumTypeAdapter<NetworkStrategy> networkStrategyRxPreferencesEnumTypeAdapter() {
    return new RxPreferencesEnumTypeAdapter<>(NetworkStrategy.class);
  }

  @Provides
  @Singleton
  @Named("high_resolution_media_network_strategy")
  Preference<NetworkStrategy> highResolutionMediaNetworkStrategyPref(
      @Named("user_prefs") RxSharedPreferences rxPrefs,
      RxPreferencesEnumTypeAdapter<NetworkStrategy> networkStrategyTypeAdapter)
  {
    return rxPrefs.getObject("high_resolution_media_network_strategy", NetworkStrategy.WIFI_ONLY, networkStrategyTypeAdapter);
  }

  @Provides
  @Named("auto_play_videos_network_strategy")
  Preference<NetworkStrategy> autoPlayVideosNetworkStrategyPref(
      @Named("user_prefs") RxSharedPreferences rxPrefs,
      RxPreferencesEnumTypeAdapter<NetworkStrategy> networkStrategyTypeAdapter)
  {
    return rxPrefs.getObject("auto_play_videos_network_strategy", NetworkStrategy.WIFI_ONLY, networkStrategyTypeAdapter);
  }

  @Provides
  @Named("open_links_in_external_browser")
  Preference<Boolean> openLinksInExternalBrowserPref(@Named("user_prefs") RxSharedPreferences rxPrefs) {
    return rxPrefs.getBoolean("open_links_in_external_browser", false);
  }

  @Provides
  @Named("cache_pre_filling_network_strategies")
  Map<CachePreFillThing, Preference<NetworkStrategy>> cachePreFillingNetworkStrategies(
      @Named("user_prefs") RxSharedPreferences rxPrefs,
      RxPreferencesEnumTypeAdapter<NetworkStrategy> adapter)
  {
    Map<CachePreFillThing, Preference<NetworkStrategy>> strategies = new HashMap<>();
    for (CachePreFillThing thing : CachePreFillThing.values()) {
      strategies.put(thing, rxPrefs.getObject("cache_pre_filling_network_strategy_for_" + thing.name(), NetworkStrategy.WIFI_ONLY, adapter));
    }
    return strategies;
  }

  @Provides
  @Named("unread_messages")
  Preference<TimeInterval> unreadMessagesPollIntervalPref(@Named("user_prefs") RxSharedPreferences rxPrefs, Moshi moshi) {
    Preference.Converter<TimeInterval> timeUnitPrefConverter = new TimeInterval.TimeUnitPrefConverter(moshi);
    TimeInterval defaultInterval = TimeInterval.create(30, TimeUnit.MINUTES);
    return rxPrefs.getObject("unread_messages_poll_interval", defaultInterval, timeUnitPrefConverter);
  }

  @Provides
  @Named("user_preferences")
  Observable<Object> provideUserPrefChanges(@Named("user_prefs") SharedPreferences sharedPrefs) {
    return Observable.create(emitter -> {
      SharedPreferences.OnSharedPreferenceChangeListener changeListener = (sharedPreferences, key) -> emitter.onNext(key);
      sharedPrefs.registerOnSharedPreferenceChangeListener(changeListener);
      emitter.setCancellable(() -> sharedPrefs.unregisterOnSharedPreferenceChangeListener(changeListener));
      changeListener.onSharedPreferenceChanged(sharedPrefs, "");    // Initial value.
    });
  }

  @Provides
  @Named("show_submission_thumbnails")
  Preference<Boolean> showSubmissionThumbnailsPref(@Named("user_prefs") RxSharedPreferences rxPrefs) {
    return rxPrefs.getBoolean("show_submission_thumbnails", true);
  }
}
