package me.saket.dank;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import me.saket.dank.ui.subreddits.SubredditActivity;
import timber.log.Timber;

/**
 * Base class for {@link JobService JobServices}.
 */
public abstract class DankJobService extends JobService {

    /**
     * IDs are stored here to prevent any accidental duplicate IDs.
     */
    protected static final int ID_SUBSCRIPTIONS_RECURRING_JOB = 0;
    protected static final int ID_SUBSCRIPTIONS_ONE_TIME_JOB = 1;

    protected static final int ID_MESSAGES_FREQUENCY_USER_SET = 2;
    protected static final int ID_MESSAGES_FREQUENCY_AGGRESSIVE = 3;
    protected static final int ID_MESSAGES_IMMEDIATELY = 4;

    protected static final int ID_MARK_MESSAGE_AS_READ = 5;
    protected static final int ID_SEND_DIRECT_MESSAGE_REPLY = 6;
    protected static final int ID_MARK_ALL_MESSAGES_AS_READ = 7;

    private CompositeDisposable onDestroyDisposables;

    protected static boolean hasPendingJobForId(Context context, long jobId) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        List<JobInfo> allPendingJobs = jobScheduler.getAllPendingJobs();

        for (JobInfo pendingJob : allPendingJobs) {
            if (pendingJob.getId() == jobId) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    protected static JobInfo getPendingJobForId(Context context, long jobId) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        List<JobInfo> allPendingJobs = jobScheduler.getAllPendingJobs();

        for (JobInfo pendingJob : allPendingJobs) {
            Timber.i("pendingJob: %s", pendingJob);
            if (pendingJob.getId() == jobId) {
                return pendingJob;
            }
        }

        return null;
    }

    protected void unsubscribeOnDestroy(Disposable subscription) {
        if (onDestroyDisposables == null) {
            onDestroyDisposables = new CompositeDisposable();
        }
        onDestroyDisposables.add(subscription);
    }

    @Override
    public void onDestroy() {
        if (onDestroyDisposables != null) {
            onDestroyDisposables.clear();
        }
        super.onDestroy();
    }

    protected void displayDebugNotification(int notifId, String notifBody, Object... args) {
        if (!BuildConfig.DEBUG) {
            Timber.e(new RuntimeException("Debug notification"), "This shouldn't be here!");
            return;
        }

        Intent homeActivityIntent = new Intent(this, SubredditActivity.class);
        PendingIntent onClickPendingIntent = PendingIntent.getActivity(this, 0, homeActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(String.format(notifBody, args))
                .setContentIntent(onClickPendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_MIN)
                .setColor(ContextCompat.getColor(getBaseContext(), R.color.color_accent));

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(notifId, builder.build());
    }

}
