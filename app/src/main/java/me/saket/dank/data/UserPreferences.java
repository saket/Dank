package me.saket.dank.data;

import android.content.SharedPreferences;
import android.support.annotation.CheckResult;
import android.text.format.DateUtils;

import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;

/**
 * Used for accessing user's preferences.
 */
@Singleton
public class UserPreferences {

  private static final long DEFAULT_INTERVAL_FOR_MESSAGES_CHECK_MILLIS = DateUtils.MINUTE_IN_MILLIS * 30;

  private static final String KEY_DEFAULT_SUBREDDIT = "defaultSubreddit";
  private static final String KEY_UNREAD_MESSAGES_CHECK_INTERVAL_MILLIS = "unreadMessagesCheckInterval";
  private static final String KEY_SHOW_SUBMISSION_COMMENTS_COUNT_IN_BYLINE = "showSubmissionCommentsCountInByline";
  private static final String KEY_CACHE_THING_PRE_FILL_NETWORK_PREFERENCE_ = "cacheThingPreFillNetworkPreference_";
  private static final String KEY_HIGH_RESOLUTION_MEDIA_NETWORK_STRATEGY = "highResolutionMediaNetworkStrategy";

  private static final boolean DEFAULT_VALUE_SHOW_SUBMISSION_COMMENTS_COUNT = false;

  private final SharedPreferences sharedPrefs;
  private final RxSharedPreferences rxPreferences;

  @Inject
  public UserPreferences(SharedPreferences sharedPrefs, RxSharedPreferences rxPreferences) {
    this.sharedPrefs = sharedPrefs;
    this.rxPreferences = rxPreferences;
  }

  public String defaultSubreddit(String valueIfNull) {
    return sharedPrefs.getString(KEY_DEFAULT_SUBREDDIT, valueIfNull);
  }

  public void setDefaultSubreddit(String subredditName) {
    sharedPrefs.edit().putString(KEY_DEFAULT_SUBREDDIT, subredditName).apply();
  }

  public long unreadMessagesCheckIntervalMillis() {
    return sharedPrefs.getLong(KEY_UNREAD_MESSAGES_CHECK_INTERVAL_MILLIS, DEFAULT_INTERVAL_FOR_MESSAGES_CHECK_MILLIS);
  }

  public void setShowSubmissionCommentsCountInByline(boolean showCommentsCount) {
    sharedPrefs.edit().putBoolean(KEY_SHOW_SUBMISSION_COMMENTS_COUNT_IN_BYLINE, showCommentsCount).apply();
  }

  public Preference<Boolean> canShowSubmissionCommentCountInByline() {
    return rxPreferences.getBoolean(KEY_SHOW_SUBMISSION_COMMENTS_COUNT_IN_BYLINE, DEFAULT_VALUE_SHOW_SUBMISSION_COMMENTS_COUNT);
  }

  /**
   * Network strategy for pre-filling <var>thing</var> in cache.
   */
  @CheckResult
  public Observable<NetworkStrategy> streamCachePreFillNetworkStrategy(CachePreFillThing thing) {
    return rxPreferences.getString(KEY_CACHE_THING_PRE_FILL_NETWORK_PREFERENCE_ + thing.name(), NetworkStrategy.WIFI_ONLY.name())
        .asObservable()
        .map(preferenceString -> NetworkStrategy.valueOf(preferenceString));
  }

  /**
   * Network strategy for loading high-resolution images and videos.
   */
  @CheckResult
  public Observable<NetworkStrategy> streamHighResolutionMediaNetworkStrategy() {
    return rxPreferences.getString(KEY_HIGH_RESOLUTION_MEDIA_NETWORK_STRATEGY, NetworkStrategy.WIFI_ONLY.name())
        .asObservable()
        .map(preferenceString -> NetworkStrategy.valueOf(preferenceString));
  }
}
