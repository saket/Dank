package me.saket.dank.ui.user;

import static io.reactivex.schedulers.Schedulers.io;

import android.content.Context;

import javax.inject.Inject;

import me.saket.dank.data.SubredditSubscriptionManager;
import me.saket.dank.data.UserPreferences;
import me.saket.dank.di.Dank;
import me.saket.dank.notifs.CheckUnreadMessagesJobService;
import me.saket.dank.ui.subreddit.SubredditSubscriptionsSyncJob;
import timber.log.Timber;

public class UserAuthListener {

  private final UserPreferences userPrefs;
  private final SubredditSubscriptionManager subscriptionManager;

  @Inject
  public UserAuthListener(UserPreferences userPrefs, SubredditSubscriptionManager subscriptionManager) {
    this.userPrefs = userPrefs;
    this.subscriptionManager = subscriptionManager;
  }

  public void handleActiveSessionOnAppStartup(Context context) {
    Timber.d("User is already logged in. Running background jobs");
    runBackgroundJobs(context);
  }

  public void handleLoggedIn(Context context) {
    Timber.d("User is logged in. Doing things.");
    runBackgroundJobs(context);

    // Reload subreddit subscriptions. Not implementing onError() is intentional.
    // This code is not supposed to fail :/
    subscriptionManager.removeAll()
        .andThen(subscriptionManager.refreshSubscriptions())
        .subscribeOn(io())
        .subscribe();
  }

  public void handleLoggedOut() {
    Timber.d("User logged out. Doing things.");
    Dank.subscriptions().removeAll()
        .subscribeOn(io())
        .subscribe();
  }

  private void runBackgroundJobs(Context context) {
    SubredditSubscriptionsSyncJob.syncImmediately(context);
    SubredditSubscriptionsSyncJob.schedule(context);

    CheckUnreadMessagesJobService.syncImmediately(context);
    CheckUnreadMessagesJobService.schedule(context, userPrefs);
  }
}
