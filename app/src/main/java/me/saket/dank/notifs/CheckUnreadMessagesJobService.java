package me.saket.dank.notifs;

import static me.saket.dank.utils.RxUtils.applySchedulersCompletable;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;
import android.text.format.DateUtils;

import net.dean.jraw.models.Message;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import me.saket.dank.DankJobService;
import me.saket.dank.data.PaginationAnchor;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.user.messages.InboxFolder;
import me.saket.dank.ui.user.messages.MessageCacheKey;
import me.saket.dank.utils.PersistableBundleUtils;
import timber.log.Timber;

/**
 * Fetches unread messages and displays a notification for them.
 */
public class CheckUnreadMessagesJobService extends DankJobService {

  private static final String KEY_SKIP_CACHE = "skipCache";

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
      JobInfo userSetSyncJob = new JobInfo.Builder(ID_MESSAGES_FREQUENCY_USER_SET, new ComponentName(context, CheckUnreadMessagesJobService.class))
          .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
          .setPersisted(true)
          .setPeriodic(userSelectedTimeIntervalMillis)
          .build();
      jobScheduler.schedule(userSetSyncJob);
    }

    boolean isAggressiveAlreadyScheduled = hasPendingJobForId(context, ID_MESSAGES_FREQUENCY_AGGRESSIVE);
    long aggressiveTimeIntervalMillis = DateUtils.MINUTE_IN_MILLIS * 15;

    if (!isAggressiveAlreadyScheduled && userSelectedTimeIntervalMillis != aggressiveTimeIntervalMillis) {
      JobInfo aggressiveSyncJob = new JobInfo.Builder(ID_MESSAGES_FREQUENCY_AGGRESSIVE, new ComponentName(context, CheckUnreadMessagesJobService.class))
          .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
          .setRequiresCharging(true)
          .setPersisted(true)
          .setPeriodic(aggressiveTimeIntervalMillis)
          .build();
      jobScheduler.schedule(aggressiveSyncJob);
    }
  }

  /**
   * Fetch unread messages and display notification immediately.
   *
   * @param skipCache Currently used for refreshing existing notifications, where doing a network call is not required.
   */
  private static void syncImmediately(Context context, boolean skipCache) {
    PersistableBundle extras = new PersistableBundle(1);
    PersistableBundleUtils.putBoolean(extras, KEY_SKIP_CACHE, skipCache);

    JobInfo syncJob = new JobInfo.Builder(ID_MESSAGES_IMMEDIATELY, new ComponentName(context, CheckUnreadMessagesJobService.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPersisted(false)
        .setOverrideDeadline(0)
        .setExtras(extras)
        .build();

    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    jobScheduler.schedule(syncJob);
  }

  /**
   * Fetch unread messages and display notification immediately.
   */
  public static void syncImmediately(Context context) {
    syncImmediately(context, false /* skipCache */);
  }

  /**
   * Like a normal sync, but this gets the unread messages from cache first, so the notifications end up
   * getting refreshed automatically. Thus the name.
   */
  public static void refreshNotifications(Context context) {
    syncImmediately(context, true /* skipCache */);
  }

  @Override
  public boolean onStartJob(JobParameters params) {
    MessageCacheKey cacheKey = MessageCacheKey.create(InboxFolder.UNREAD, PaginationAnchor.createEmpty());
    boolean skipCache = PersistableBundleUtils.getBoolean(params.getExtras(), KEY_SKIP_CACHE);

    Single<List<Message>> stream = skipCache
        ? Dank.stores().messageStore().get(cacheKey)
        : Dank.stores().messageStore().fetch(cacheKey);

    Timber.i("Fetching unread messages");

    unsubscribeOnDestroy(stream
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
    Timber.i("Notifying for unread messages");

    return notifManager.filterUnseenMessages(unreadMessages)
        .flatMapCompletable(unseenMessages ->
            unseenMessages.isEmpty()
                ? notifManager.dismissAllNotifications(getBaseContext())
                : notifManager.displayNotification(getBaseContext(), unseenMessages)
        );
  }

}
