package me.saket.dank;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import me.saket.dank.ui.subreddit.SubredditActivity;
import me.saket.dank.utils.lifecycle.LifecycleStreams;

/**
 * Base class for {@link JobService JobServices}.
 */
public abstract class DankJobService extends JobService {

  /**
   * IDs are stored here to prevent any accidental duplicate IDs.
   */
  protected static final int ID_GENERIC_DEBUG = -99;
  protected static final int ID_DEBUG_RECYCLING = -98;

  protected static final int ID_SUBSCRIPTIONS_RECURRING_JOB = 0;
  protected static final int ID_SUBSCRIPTIONS_ONE_TIME_JOB = 1;

  protected static final int ID_MESSAGES_USER_SCHEDULED = 2;
  protected static final int ID_MESSAGES_AGGRESSIVE = 3;
  protected static final int ID_MESSAGES_IMMEDIATELY = 4;

  protected static final int ID_MARK_MESSAGE_AS_READ = 5;
  protected static final int ID_SEND_DIRECT_MESSAGE_REPLY = 6;
  protected static final int ID_MARK_ALL_MESSAGES_AS_READ = 7;

  protected static final int ID_VOTE = 8;

  protected static final int ID_RETRY_REPLY = 9;

  protected static final int ID_RECYCLE_OLD_SUBMISSIONS = 10;

  private CompositeDisposable onDestroyDisposables;
  private Relay<Object> onDestroyStream = PublishRelay.create();

  @Override
  public final boolean onStartJob(JobParameters params) {
    JobStartCallback callback = onStartJob2(params);
    return callback.isRunningInBackground();
  }

  @Override
  public final boolean onStopJob(JobParameters params) {
    JobStopCallback callback = onStopJob2();
    return callback.shouldReschedule();
  }

  public abstract JobStartCallback onStartJob2(JobParameters params);

  public abstract JobStopCallback onStopJob2();

  @Deprecated
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
    onDestroyStream.accept(LifecycleStreams.NOTHING);
    super.onDestroy();
  }

  protected void displayDebugNotification(String notifBody, Object... args) {
    displayDebugNotification(ID_GENERIC_DEBUG, notifBody, args);
  }

  protected void displayDebugNotification(int id, String notifBody, Object... args) {
    if (!BuildConfig.DEBUG) {
      return;
    }

    Intent homeActivityIntent = new Intent(this, SubredditActivity.class);
    PendingIntent onClickPendingIntent = PendingIntent.getActivity(this, 0, homeActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT);

    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, getString(R.string.notification_channel_debug_notifs_id))
        .setContentTitle(String.format(notifBody, args))
        .setContentIntent(onClickPendingIntent)
        .setSmallIcon(R.drawable.ic_status_bar_24dp)
        .setShowWhen(true)
        .setAutoCancel(true)
        .setPriority(Notification.PRIORITY_MIN)
        .setColor(ContextCompat.getColor(getBaseContext(), R.color.color_accent));

    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    //noinspection ConstantConditions
    notificationManager.notify(id, builder.build());
  }

  protected void removeDebugNotification() {
    if (!BuildConfig.DEBUG) {
      return;
    }
    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    //noinspection ConstantConditions
    notificationManager.cancel(ID_GENERIC_DEBUG);
  }

  public Observable<Object> lifecycleOnDestroy() {
    return onDestroyStream.take(1);
  }

  @AutoValue
  public abstract static class JobStartCallback {
    public abstract boolean isRunningInBackground();

    public static JobStartCallback runningInBackground() {
      return new AutoValue_DankJobService_JobStartCallback(true);
    }

    public static JobStartCallback finished() {
      return new AutoValue_DankJobService_JobStartCallback(false);
    }
  }

  @AutoValue
  public abstract static class JobStopCallback {
    public abstract boolean shouldReschedule();

    public static JobStopCallback rescheduleRequired() {
      return new AutoValue_DankJobService_JobStopCallback(true);
    }

    public static JobStopCallback drop() {
      return new AutoValue_DankJobService_JobStopCallback(false);
    }
  }
}
