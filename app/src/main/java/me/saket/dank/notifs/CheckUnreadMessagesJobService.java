package me.saket.dank.notifs;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;

import com.f2prateek.rx.preferences2.Preference;

import net.dean.jraw.models.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Completable;
import me.saket.dank.DankJobService;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.InboxRepository;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.preferences.NetworkStrategy;
import me.saket.dank.ui.user.messages.InboxFolder;
import me.saket.dank.utils.Arrays2;
import me.saket.dank.utils.PersistableBundleUtils;
import me.saket.dank.utils.TimeInterval;
import timber.log.Timber;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;
import static java.util.Collections.unmodifiableList;

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
  public static void schedule(Context context, Preference<TimeInterval> pollInterval, Preference<NetworkStrategy> pollNetworkStrategy) {
    JobInfo.Builder userJobBuilder = new JobInfo.Builder(ID_MESSAGES_USER_SCHEDULED, new ComponentName(context, CheckUnreadMessagesJobService.class))
        .setPersisted(true)
        .setPeriodic(pollInterval.get().intervalMillis());

    userJobBuilder = pollNetworkStrategy.get().setNetworkRequirement(userJobBuilder);
    JobInfo userSetSyncJob = userJobBuilder.build();

    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    //noinspection ConstantConditions
    jobScheduler.schedule(userSetSyncJob);

    TimeInterval aggressiveTimeInterval = TimeInterval.create(15, TimeUnit.MINUTES);

    //Timber.i("User selected interval: %s", pollInterval.get());
    //Timber.i("Aggressive time interval: %s", aggressiveTimeInterval);

    if (!pollInterval.get().equals(aggressiveTimeInterval)) {
      JobInfo aggressiveSyncJob = new JobInfo.Builder(ID_MESSAGES_AGGRESSIVE, new ComponentName(context, CheckUnreadMessagesJobService.class))
          .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
          .setRequiresCharging(true)
          .setPersisted(true)
          .setPeriodic(aggressiveTimeInterval.intervalMillis())
          .build();
      //Timber.i("Scheduling aggressive job");
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

  public static void unSchedule(Context context) {
    //Timber.i("Disabling message polling");
    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    //noinspection ConstantConditions
    jobScheduler.cancel(ID_MESSAGES_USER_SCHEDULED);
    jobScheduler.cancel(ID_MESSAGES_AGGRESSIVE);
    jobScheduler.cancel(ID_MESSAGES_IMMEDIATELY);
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
  public JobStartCallback onStartJob2(JobParameters params) {
    displayDebugNotification("Checking for unread messages");

    //Timber.i("Fetching unread messages. JobID: %s", params.getJobId());
    boolean shouldRefreshMessages = PersistableBundleUtils.getBoolean(params.getExtras(), KEY_REFRESH_MESSAGES);

    //Timber.i("Checking for unread messages");

    Completable refreshCompletable;
    if (shouldRefreshMessages) {
      //Timber.i("Refreshing msgs");
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
        //.doOnSuccess(unreads -> Timber.i("Found %s unreads", unreads.size()))
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

    return JobStartCallback.runningInBackground();
  }

  @Override
  public JobStopCallback onStopJob2() {
    return JobStopCallback.rescheduleRequired();
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
