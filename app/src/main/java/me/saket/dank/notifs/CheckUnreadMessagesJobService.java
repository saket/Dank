package me.saket.dank.notifs;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;
import static java.util.Collections.unmodifiableList;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;
import android.text.format.DateUtils;

import net.dean.jraw.models.Message;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import io.reactivex.Completable;
import me.saket.dank.DankJobService;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.InboxRepository;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.data.UserPreferences;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.user.messages.InboxFolder;
import me.saket.dank.utils.Arrays2;
import me.saket.dank.utils.PersistableBundleUtils;
import timber.log.Timber;

/**
 * Fetches unread messages and displays a notification for them.
 */
public class CheckUnreadMessagesJobService extends DankJobService {

  private static final String KEY_REFRESH_MESSAGES = "refreshMessages";

  @Inject InboxRepository inboxRepository;
  @Inject ErrorResolver errorResolver;
  @Inject MessagesNotificationManager messagesNotifManager;

  /**
   * Schedules two recurring sync jobs:
   * <p>
   * 1. One that is infrequent and uses user set time period. This runs on battery and metered connections.
   * 2. Another one that is more frequent, but runs only when the device is on an unmetered connection and charging.
   */
  public static void schedule(Context context, UserPreferences userPrefs) {
    long userSelectedTimeIntervalMillis = userPrefs.unreadMessagesCheckIntervalMillis();
    JobInfo userSetSyncJob = new JobInfo.Builder(ID_MESSAGES_FREQUENCY_USER_SET, new ComponentName(context, CheckUnreadMessagesJobService.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPersisted(true)
        .setPeriodic(userSelectedTimeIntervalMillis)
        .build();

    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    //noinspection ConstantConditions
    jobScheduler.schedule(userSetSyncJob);

    long aggressiveTimeIntervalMillis = DateUtils.MINUTE_IN_MILLIS * 15;

    Timber.i("userSelectedTimeIntervalMillis: %s", userSelectedTimeIntervalMillis);
    Timber.i("aggressiveTimeIntervalMillis: %s", aggressiveTimeIntervalMillis);

    if (userSelectedTimeIntervalMillis != aggressiveTimeIntervalMillis) {
      JobInfo aggressiveSyncJob = new JobInfo.Builder(ID_MESSAGES_FREQUENCY_AGGRESSIVE, new ComponentName(context, CheckUnreadMessagesJobService.class))
          .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
          .setRequiresCharging(true)
          .setPersisted(true)
          .setPeriodic(aggressiveTimeIntervalMillis)
          .build();
      Timber.i("Scheduling aggressive job");
      jobScheduler.schedule(aggressiveSyncJob);
    }
  }

  /**
   * Fetch unread messages and display notification immediately.
   */
  public static void syncImmediately(Context context) {
    syncImmediately(context, true);
  }

  /**
   * Fetch unread messages and display notification immediately.
   *
   * @param refreshMessages Whether to fetch new unread messages before displaying the notifications.
   */
  private static void syncImmediately(Context context, boolean refreshMessages) {
    PersistableBundle extras = new PersistableBundle(1);
    PersistableBundleUtils.putBoolean(extras, KEY_REFRESH_MESSAGES, refreshMessages);

    JobInfo syncJob = new JobInfo.Builder(ID_MESSAGES_IMMEDIATELY, new ComponentName(context, CheckUnreadMessagesJobService.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPersisted(false)
        .setOverrideDeadline(0)
        .setExtras(extras)
        .build();

    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    //noinspection ConstantConditions
    jobScheduler.schedule(syncJob);
  }

  /**
   * Refresh notifications from the DB. Doesn't hit the network.
   */
  public static void refreshNotifications(Context context) {
    syncImmediately(context, false);
  }

  @Override
  public void onCreate() {
    Dank.dependencyInjector().inject(this);
    super.onCreate();
  }

  @Override
  public boolean onStartJob(JobParameters params) {
    displayDebugNotification("Checking for unread messages");

    //Timber.i("Fetching unread messages. JobID: %s", params.getJobId());
    boolean shouldRefreshMessages = PersistableBundleUtils.getBoolean(params.getExtras(), KEY_REFRESH_MESSAGES);

    Timber.i("Checking for unread messages");

    Completable refreshCompletable;
    if (shouldRefreshMessages) {
      Timber.i("Refreshing msgs");
      refreshCompletable = inboxRepository.messages(InboxFolder.UNREAD)
          .firstOrError()
          .flatMapCompletable(existingUnreads -> inboxRepository.refreshMessages(InboxFolder.UNREAD, false)
              .map(receivedUnreads -> {
                List<Message> staleMessages = new ArrayList<>(existingUnreads.size());
                staleMessages.addAll(existingUnreads);
                staleMessages.removeAll(receivedUnreads);

//              Timber.i("----------------------------------------");
//              Timber.w("Stale notifs: %s", staleMessages.size());
//              for (Message m : staleMessages) {
//                Timber.i("%s", m.getBody().substring(0, Math.min(m.getBody().length(), 50)));
//              }
//              Timber.i("----------------------------------------");

                return unmodifiableList(staleMessages);
              })
              .flatMapCompletable(staleMessages ->
                  // When generating bundled notifications, Android does not remove existing bundle when a new bundle is posted.
                  // It instead amends any new notifications with the existing ones. This means that we'll have to manually
                  // cleanup stale notifications.
                  messagesNotifManager.dismissNotification(getBaseContext(), Arrays2.toArray(staleMessages, Message.class))
              ));
    } else {
      refreshCompletable = Completable.complete();
    }

    refreshCompletable
        .andThen(inboxRepository.messages(InboxFolder.UNREAD).firstOrError())
        .subscribeOn(io())
        .observeOn(mainThread())
        .doOnSuccess(unreads -> Timber.i("Found %s unreads", unreads.size()))
        .flatMapCompletable(unreads -> notifyUnreadMessages(unreads))
        .ambWith(lifecycleOnDestroy().ignoreElements())
        .subscribe(
            () -> jobFinished(params, false),
            error -> {
              ResolvedError resolvedError = errorResolver.resolve(error);
              if (resolvedError.isUnknown()) {
                Timber.e(error, "Unknown error while fetching unread messages.");
              }

              boolean needsReschedule = resolvedError.isNetworkError() || resolvedError.isRedditServerError();
              jobFinished(params, needsReschedule);
            }
        );

    // Return true to indicate that the job is still being processed (in a background thread).
    return true;
  }

  @Override
  public boolean onStopJob(JobParameters params) {
    // Return true to indicate JobScheduler that the job should be rescheduled.
    return true;
  }

  private Completable notifyUnreadMessages(List<Message> unreadMessages) {
    return messagesNotifManager.filterUnseenMessages(unreadMessages)
        .flatMapCompletable(unseenMessages -> {
          if (unseenMessages.isEmpty()) {
            displayDebugNotification("No unread messages found");
            return messagesNotifManager.dismissAllNotifications(getBaseContext());
          } else {
            removeDebugNotification();
            return messagesNotifManager.displayNotification(getBaseContext(), unseenMessages);
          }
        });
  }
}
