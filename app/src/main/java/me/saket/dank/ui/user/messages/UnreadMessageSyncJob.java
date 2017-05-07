package me.saket.dank.ui.user.messages;

import static me.saket.dank.utils.RxUtils.applySchedulersSingle;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.text.format.DateUtils;

import me.saket.dank.DankJobService;
import me.saket.dank.data.PaginationAnchor;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
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

        JobInfo existingUserSetJobInfo = getPendingJobForId(context, ID_MESSAGES_FREQUENCY_AGGRESSIVE);
        boolean isUserSetJobAlreadyScheduled = existingUserSetJobInfo != null;
        boolean wasIntervalChanged = isUserSetJobAlreadyScheduled && existingUserSetJobInfo.getIntervalMillis() != userSelectedTimeIntervalMillis;

        if (!isUserSetJobAlreadyScheduled || wasIntervalChanged) {
            JobInfo frequentSyncJob = new JobInfo.Builder(ID_MESSAGES_FREQUENCY_USER_SET, new ComponentName(context, UnreadMessageSyncJob.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                    .setRequiresCharging(true)
                    .setPersisted(true)
                    .setPeriodic(userSelectedTimeIntervalMillis)
                    .build();
            jobScheduler.schedule(frequentSyncJob);
        }

        boolean isAggressiveAlreadyScheduled = hasPendingJobForId(context, ID_MESSAGES_FREQUENCY_USER_SET);
        long frequentTimePeriod = DateUtils.MINUTE_IN_MILLIS * 5;

        if (!isAggressiveAlreadyScheduled && userSelectedTimeIntervalMillis != frequentTimePeriod) {
            JobInfo infrequentSyncJob = new JobInfo.Builder(ID_MESSAGES_FREQUENCY_AGGRESSIVE, new ComponentName(context, UnreadMessageSyncJob.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .setPeriodic(frequentTimePeriod)
                    .build();
            jobScheduler.schedule(infrequentSyncJob);
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Timber.i("Fetching unread messages. Is frequent job? %s", (params.getJobId() == ID_MESSAGES_FREQUENCY_AGGRESSIVE));

        MessageCacheKey cacheKey = MessageCacheKey.create(InboxFolder.UNREAD, PaginationAnchor.createEmpty());
        unsubscribeOnDestroy(Dank.stores().messageStore().fetch(cacheKey)
                .compose(applySchedulersSingle())
                .subscribe(unreadMessages -> {
                    if (!unreadMessages.isEmpty()) {
                        displayDebugNotification("You have %s unread messages", unreadMessages.size());
                    }
                    jobFinished(params, false /* needsReschedule */);

                }, error -> {
                    ResolvedError resolvedError = Dank.errors().resolve(error);
                    if (resolvedError.isUnknown()) {
                        Timber.e(error, "Unknown error while fetching unread messages.");
                    }

                    boolean needsReschedule = resolvedError.isNetworkError() || resolvedError.isRedditServerError();
                    jobFinished(params, needsReschedule);
                }));

        // Plan:
        // 1. DONE. Fetch all unreads.
        // 2. How do we figure out which ones we haven't notified?
        // 3. Show notification.

        // Return true to indicate that the job is still being processed (in a background thread).
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // Return true to indicate JobScheduler that the job should be rescheduled.
        return false;
    }

}
