package me.saket.dank.ui.user;

import static io.reactivex.schedulers.Schedulers.io;

import android.content.Context;
import androidx.annotation.CheckResult;
import androidx.annotation.VisibleForTesting;

import com.f2prateek.rx.preferences2.Preference;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import me.saket.dank.analytics.CrashReporter;
import me.saket.dank.notifs.CheckUnreadMessagesJobService;
import me.saket.dank.ui.preferences.NetworkStrategy;
import me.saket.dank.ui.subscriptions.SubredditSubscriptionsSyncJob;
import me.saket.dank.ui.subscriptions.SubscriptionRepository;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.TimeInterval;
import timber.log.Timber;

@Singleton
public class UserAuthListener {

  private final Lazy<SubscriptionRepository> subscriptionRepository;
  private final Lazy<UserSessionRepository> userSessionRepository;
  private Lazy<CrashReporter> crashReporter;
  private final Lazy<Preference<Boolean>> unreadMessagesPollEnabledPref;
  private final Lazy<Preference<TimeInterval>> unreadMessagesPollInterval;
  private final Lazy<Preference<NetworkStrategy>> unreadMessagesPollNetworkStrategy;

  @Inject
  public UserAuthListener(
      Lazy<SubscriptionRepository> subscriptionRepository,
      Lazy<UserSessionRepository> userSessionRepository,
      Lazy<CrashReporter> crashReporter,
      @Named("unread_messages") Lazy<Preference<Boolean>> unreadMessagesPollEnabledPref,
      @Named("unread_messages") Lazy<Preference<TimeInterval>> unreadMessagesPollInterval,
      @Named("unread_messages") Lazy<Preference<NetworkStrategy>> unreadMessagesPollNetworkStrategy
  )
  {
    this.crashReporter = crashReporter;
    this.unreadMessagesPollEnabledPref = unreadMessagesPollEnabledPref;
    this.unreadMessagesPollInterval = unreadMessagesPollInterval;
    this.subscriptionRepository = subscriptionRepository;
    this.userSessionRepository = userSessionRepository;
    this.unreadMessagesPollNetworkStrategy = unreadMessagesPollNetworkStrategy;
  }

  @CheckResult
  public Completable startListening(Context context) {
    Observable<Optional<UserSession>> userSessions = userSessionRepository.get().streamSessions()
        .replay(1)
        .refCount();

    Completable appStartupCompletable = userSessions
        .take(1)  // Initial value is immediately emitted.
        .filter(Optional::isPresent)
        .delay(2, TimeUnit.SECONDS)
        .flatMapCompletable(o -> Completable.fromAction(() -> handleActiveSessionOnAppStartup(context)));

    Completable logInCompletable = userSessions
        .skip(1)  // Don't want a log-in callback on app startup.
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMapCompletable(userSession -> Completable.fromAction(() -> handleLoggedIn(context, userSession)));

    Completable logOutCompletable = userSessions
        .doOnNext(o -> Timber.w("session: %s", o))
        .skip(1)
        .filter(Optional::isEmpty)
        .flatMapCompletable(o -> Completable.fromAction(() -> handleLoggedOut()));

    return Completable.mergeArrayDelayError(appStartupCompletable, logInCompletable, logOutCompletable);
  }

  @VisibleForTesting
  void handleActiveSessionOnAppStartup(Context context) {
    //Timber.d("User is already logged in. Running background jobs");
    runBackgroundJobs(context);
  }

  @VisibleForTesting
  void handleLoggedIn(Context context, UserSession userSession) {
    //Timber.d("User is logged in. Doing things.");

    crashReporter.get().identifyUser(userSession.username());

    // Reload subreddit subscriptions. Not implementing onError() is intentional.
    // This code is not supposed to fail :/
    subscriptionRepository.get().removeAll()
        .andThen(subscriptionRepository.get().refreshAndSaveSubscriptions())
        .subscribeOn(io())
        .subscribe();

    runBackgroundJobs(context);
  }

  @VisibleForTesting
  void handleLoggedOut() {
    //Timber.d("User logged out. Doing things.");

    crashReporter.get().identifyUser(null);

    subscriptionRepository.get().removeAll()
        .subscribeOn(io())
        .subscribe(() -> {
          Timber.i("Default sub set to: %s", subscriptionRepository.get().defaultSubreddit());
        });
  }

  void runBackgroundJobs(Context context) {
    SubredditSubscriptionsSyncJob.syncImmediately(context);
    SubredditSubscriptionsSyncJob.schedule(context);

    Boolean isMessagePollingEnabled = unreadMessagesPollEnabledPref.get().get();
    if (isMessagePollingEnabled) {
      CheckUnreadMessagesJobService.syncImmediately(context);
      CheckUnreadMessagesJobService.schedule(context, unreadMessagesPollInterval.get(), unreadMessagesPollNetworkStrategy.get());
    } else {
      CheckUnreadMessagesJobService.unSchedule(context);
    }
  }
}
