package me.saket.dank.ui.user;

import static io.reactivex.schedulers.Schedulers.io;

import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.VisibleForTesting;

import com.f2prateek.rx.preferences2.Preference;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Observable;
import me.saket.dank.data.SubredditSubscriptionManager;
import me.saket.dank.notifs.CheckUnreadMessagesJobService;
import me.saket.dank.ui.subscriptions.SubredditSubscriptionsSyncJob;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.TimeInterval;
import timber.log.Timber;

@Singleton
public class UserAuthListener {

  private final Preference<TimeInterval> unreadMessagesPollInterval;
  private final SubredditSubscriptionManager subscriptionManager;
  private final UserSessionRepository userSessionRepository;

  @Inject
  public UserAuthListener(
      SubredditSubscriptionManager subscriptionManager,
      UserSessionRepository userSessionRepository,
      @Named("unread_messages") Preference<TimeInterval> unreadMessagesPollInterval)
  {
    this.unreadMessagesPollInterval = unreadMessagesPollInterval;
    this.subscriptionManager = subscriptionManager;
    this.userSessionRepository = userSessionRepository;
  }

  @Override
  protected void finalize() throws Throwable {
    Timber.i("GCCCCCC");
    super.finalize();
  }

  @CheckResult
  public Completable doSomething(Context context) {
    Observable<Optional<UserSession>> userSessions = userSessionRepository.streamSessions()
        .delay(5, TimeUnit.SECONDS)
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
        .flatMapCompletable(o -> Completable.fromAction(() -> handleLoggedIn(context)));

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
  void handleLoggedIn(Context context) {
    //Timber.d("User is logged in. Doing things.");

    // Reload subreddit subscriptions. Not implementing onError() is intentional.
    // This code is not supposed to fail :/
    subscriptionManager.removeAll()
        .andThen(subscriptionManager.refreshAndSaveSubscriptions())
        .subscribeOn(io())
        .subscribe();

    runBackgroundJobs(context);
  }

  @VisibleForTesting
  void handleLoggedOut() {
    //Timber.d("User logged out. Doing things.");

    subscriptionManager.removeAll()
        .subscribeOn(io())
        .subscribe(() -> {
          Timber.i("Default sub set to: %s", subscriptionManager.defaultSubreddit());
        });
  }

  @VisibleForTesting
  void runBackgroundJobs(Context context) {
    SubredditSubscriptionsSyncJob.syncImmediately(context);
    SubredditSubscriptionsSyncJob.schedule(context);

    CheckUnreadMessagesJobService.syncImmediately(context);
    CheckUnreadMessagesJobService.schedule(context, unreadMessagesPollInterval);
  }
}
