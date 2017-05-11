package me.saket.dank.notifs;

import static me.saket.dank.utils.RxUtils.applySchedulersCompletable;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.text.format.DateUtils;

import net.dean.jraw.models.Message;

import java.util.List;

import io.reactivex.Completable;
import me.saket.dank.DankJobService;
import me.saket.dank.data.PaginationAnchor;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.user.messages.InboxFolder;
import me.saket.dank.ui.user.messages.MessageCacheKey;
import timber.log.Timber;

/**
 * Fetches unread messages.
 */
public class UnreadMessageSyncJob extends DankJobService {

  /**
   * Schedules two recurring sync jobs:
   * <p>
   * 1. One that is infrequent and uses user set time period. This runs on battery and metered connections.
   * 2. Another one that is more frequent, but runs only when the device is on a metered connection and charging.
   */
  public static void schedule(Context context) {
    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    long userSelectedTimeIntervalMillis = Dank.userPrefs().unreadMessagesCheckIntervalMillis();

    // TODO: existingUserSetJobInfo always returns null on the emulator. If it's the same case on phone, remove all this extra crappy logic.
    JobInfo existingUserSetJobInfo = getPendingJobForId(context, ID_MESSAGES_FREQUENCY_USER_SET);
    boolean isUserSetJobAlreadyScheduled = existingUserSetJobInfo != null;
    boolean wasIntervalChanged = isUserSetJobAlreadyScheduled && existingUserSetJobInfo.getIntervalMillis() != userSelectedTimeIntervalMillis;

    if (!isUserSetJobAlreadyScheduled || wasIntervalChanged) {
      JobInfo userSetSyncJob = new JobInfo.Builder(ID_MESSAGES_FREQUENCY_USER_SET, new ComponentName(context, UnreadMessageSyncJob.class))
          .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
          .setPersisted(true)
          .setPeriodic(userSelectedTimeIntervalMillis)
          .build();
      jobScheduler.schedule(userSetSyncJob);
    }

    boolean isAggressiveAlreadyScheduled = hasPendingJobForId(context, ID_MESSAGES_FREQUENCY_AGGRESSIVE);
    long aggressiveTimeIntervalMillis = DateUtils.MINUTE_IN_MILLIS * 15;

    if (!isAggressiveAlreadyScheduled && userSelectedTimeIntervalMillis != aggressiveTimeIntervalMillis) {
      JobInfo aggressiveSyncJob = new JobInfo.Builder(ID_MESSAGES_FREQUENCY_AGGRESSIVE, new ComponentName(context, UnreadMessageSyncJob.class))
          .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
          .setRequiresCharging(true)
          .setPersisted(true)
          .setPeriodic(aggressiveTimeIntervalMillis)
          .build();
      jobScheduler.schedule(aggressiveSyncJob);
    }
  }

  public static void runImmediately(Context context) {
    JobInfo syncJob = new JobInfo.Builder(ID_MESSAGES_IMMEDIATELY, new ComponentName(context, UnreadMessageSyncJob.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPersisted(false)
        .setOverrideDeadline(0)
        .build();

    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    jobScheduler.schedule(syncJob);
  }

  @Override
  public boolean onStartJob(JobParameters params) {
    MessageCacheKey cacheKey = MessageCacheKey.create(InboxFolder.UNREAD, PaginationAnchor.createEmpty());
    unsubscribeOnDestroy(Dank.stores().messageStore()
        .fetch(cacheKey)
        .flatMapCompletable(unreads -> notifyUnreadMessages(unreads))
        .compose(applySchedulersCompletable())
        .subscribe(() -> {
          jobFinished(params, false /* needsReschedule */);

        }, error -> {
          ResolvedError resolvedError = Dank.errors().resolve(error);
          if (resolvedError.isUnknown()) {
            Timber.e(error, "Unknown error while fetching unread messages.");
          }

          boolean needsReschedule = resolvedError.isNetworkError() || resolvedError.isRedditServerError();
          jobFinished(params, needsReschedule);
        }));

    // Return true to indicate that the job is still being processed (in a background thread).
    return true;
  }

  @Override
  public boolean onStopJob(JobParameters params) {
    // Return true to indicate JobScheduler that the job should be rescheduled.
    return false;
  }

  private Completable notifyUnreadMessages(List<Message> unreadMessages) {
    MessagesNotificationManager notifManager = Dank.messagesNotifManager();
    return notifManager
        .updateSeenStatusOfUnreadMessagesWith(unreadMessages)
        .andThen(notifManager.filterUnseenMessages(unreadMessages))
        .flatMapCompletable(unseenMessages ->
            unseenMessages.isEmpty()
                ? notifManager.dismissAllNotifications(getBaseContext())
                : notifManager.displayNotification(getBaseContext(), unseenMessages)
        );
  }

}
