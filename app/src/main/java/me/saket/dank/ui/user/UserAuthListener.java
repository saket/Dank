package me.saket.dank.ui.user;

import static io.reactivex.schedulers.Schedulers.io;

import android.content.Context;

import com.f2prateek.rx.preferences2.Preference;

import javax.inject.Inject;
import javax.inject.Named;

import me.saket.dank.data.SubredditSubscriptionManager;
import me.saket.dank.di.Dank;
import me.saket.dank.notifs.CheckUnreadMessagesJobService;
import me.saket.dank.ui.subreddit.SubredditSubscriptionsSyncJob;
import me.saket.dank.utils.TimeInterval;
import timber.log.Timber;

public class UserAuthListener {

  private final SubredditSubscriptionManager subscriptionManager;
  private final Preference<TimeInterval> unreadMessagesPollInterval;

  @Inject
  public UserAuthListener(
      SubredditSubscriptionManager subscriptionManager,
      @Named("unread_messages") Preference<TimeInterval> unreadMessagesPollInterval)
  {
    this.subscriptionManager = subscriptionManager;
    this.unreadMessagesPollInterval = unreadMessagesPollInterval;
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
    CheckUnreadMessagesJobService.schedule(context, unreadMessagesPollInterval);
  }
}
