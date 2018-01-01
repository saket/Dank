package me.saket.dank;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobService;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import me.saket.dank.ui.subreddits.SubredditActivity;
import me.saket.dank.utils.lifecycle.LifecycleStreams;

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

  protected static final int ID_VOTE = 8;

  protected static final int ID_RETRY_REPLY = 9;

  protected static final int ID_RECYCLE_OLD_SUBMISSIONS = 10;

  private CompositeDisposable onDestroyDisposables;
  private Relay<Object> onDestroyStream = PublishRelay.create();

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

  protected void displayDebugNotification(int notifId, String notifBody, Object... args) {
    if (!BuildConfig.DEBUG) {
      throw new RuntimeException("Debug notif: this shouldn't be here!");
    }

    Intent homeActivityIntent = new Intent(this, SubredditActivity.class);
    PendingIntent onClickPendingIntent = PendingIntent.getActivity(this, 0, homeActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT);

    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, getString(R.string.notification_channel_debug_notifs_id))
        .setContentTitle(String.format(notifBody, args))
        .setContentIntent(onClickPendingIntent)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setShowWhen(true)
        .setAutoCancel(true)
        .setPriority(Notification.PRIORITY_MIN)
        .setColor(ContextCompat.getColor(getBaseContext(), R.color.color_accent));

    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    //noinspection ConstantConditions
    notificationManager.notify(notifId, builder.build());
  }

  public Observable<Object> lifecycleOnDestroy() {
    return onDestroyStream;
  }
}
