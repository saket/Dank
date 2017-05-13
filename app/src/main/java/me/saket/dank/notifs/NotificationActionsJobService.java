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

import io.reactivex.Completable;
import io.reactivex.functions.Consumer;
import me.saket.dank.DankJobService;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.utils.JacksonHelper;
import me.saket.dank.utils.JrawUtils;
import timber.log.Timber;

/**
 * We're using JobScheduler for executing notification actions so that it also handles auto-retrying when
 * there's no network or reddit's servers are down.
 */
public class NotificationActionsJobService extends DankJobService {

  private static final String KEY_MESSAGE_JSON = "message";
  private static final String KEY_MESSAGE_DIRECT_REPLY = "messageDirectReply";
  private static final String KEY_ACTION = "action";

  private static final String ACTION_SEND_DIRECT_REPLY = "sendDirectReply";
  private static final String ACTION_MARK_MESSAGE_AS_READ = "markMessageAsRead";
  private static final String ACTION_MARK_ALL_MESSAGES_AS_READ = "markAllMessagesAsRead";

  public static void sendDirectReply(Context context, Message replyToMessage, JacksonHelper jacksonHelper, String replyText) {
    PersistableBundle extras = new PersistableBundle(2);
    extras.putString(KEY_MESSAGE_JSON, jacksonHelper.toJson(replyToMessage));
    extras.putString(KEY_MESSAGE_DIRECT_REPLY, replyText);
    extras.putString(KEY_ACTION, ACTION_SEND_DIRECT_REPLY);

    // Each job needs a unique ID and since we create a separate job for each message.
    int jobId = ID_SEND_DIRECT_MESSAGE_REPLY + replyToMessage.getId().hashCode();

    JobInfo markAsReadJob = new JobInfo.Builder(jobId, new ComponentName(context, NotificationActionsJobService.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPersisted(true)
        .setExtras(extras)
        .setOverrideDeadline(0)
        .build();
    // JobInfo use a default back-off criteria of (30s, exponential) so we're not setting
    // it ourselves.

    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    jobScheduler.schedule(markAsReadJob);
  }

  public static void markAsRead(Context context, Message message, JacksonHelper jacksonHelper) {
    PersistableBundle extras = new PersistableBundle(1);
    extras.putString(KEY_MESSAGE_JSON, jacksonHelper.toJson(message));
    extras.putString(KEY_ACTION, ACTION_MARK_MESSAGE_AS_READ);

    int jobId = ID_MARK_MESSAGE_AS_READ + message.getId().hashCode();

    JobInfo markAsReadJob = new JobInfo.Builder(jobId, new ComponentName(context, NotificationActionsJobService.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPersisted(true)
        .setExtras(extras)
        .setOverrideDeadline(DateUtils.MINUTE_IN_MILLIS * 5)
        .build();

    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    jobScheduler.schedule(markAsReadJob);
  }

  public static void markAllAsRead(Context context) {
    PersistableBundle extras = new PersistableBundle(1);
    extras.putString(KEY_ACTION, ACTION_MARK_ALL_MESSAGES_AS_READ);

    JobInfo markAsReadJob = new JobInfo.Builder(ID_MARK_ALL_MESSAGES_AS_READ, new ComponentName(context, NotificationActionsJobService.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPersisted(true)
        .setExtras(extras)
        .setOverrideDeadline(DateUtils.MINUTE_IN_MILLIS * 5)
        .build();
    // JobInfo use a default back-off criteria of (30s, exponential) so we're not setting
    // it ourselves.

    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    jobScheduler.schedule(markAsReadJob);
  }

  @Override
  public boolean onStartJob(JobParameters params) {
    // Switching on the action because a new job is created for each message, each with an unique jobId.
    //noinspection ConstantConditions
    switch (params.getExtras().getString(KEY_ACTION)) {
      case ACTION_SEND_DIRECT_REPLY:
        String replyText = params.getExtras().getString(KEY_MESSAGE_DIRECT_REPLY);
        sendDirectMessageReply(params, parseMessage(params.getExtras().getString(KEY_MESSAGE_JSON)), replyText);
        break;

      case ACTION_MARK_MESSAGE_AS_READ:
        markMessageAsRead(params, parseMessage(params.getExtras().getString(KEY_MESSAGE_JSON)));
        break;

      case ACTION_MARK_ALL_MESSAGES_AS_READ:
        markAllMessagesAsRead(params);
        break;

      default:
        throw new UnsupportedOperationException("Unknown job action: " + params.getExtras().getString(KEY_ACTION));
    }

    // Return true to indicate that the job is still being processed (in a background thread).
    return true;
  }

  private void sendDirectMessageReply(JobParameters params, Message replyToMessage, String replyText) {
    unsubscribeOnDestroy(
        Dank.reddit().withAuth(Completable.fromAction(() -> Dank.reddit().userAccountManager().reply(replyToMessage, replyText)))
            .andThen(Dank.messagesNotifManager().removeMessageNotifSeenStatus(replyToMessage))
            .compose(applySchedulersCompletable())
            .subscribe(
                () -> jobFinished(params, false /* needsReschedule */),
                rescheduleJobIfNetworkOrRedditError(params)
            )
    );
  }

  private void markMessageAsRead(JobParameters params, Message message) {
    unsubscribeOnDestroy(
        Dank.reddit().withAuth(Completable.fromAction(() -> Dank.reddit().userInboxManager().setRead(true, message)))
            .andThen(Dank.messagesNotifManager().removeMessageNotifSeenStatus(message))
            .compose(applySchedulersCompletable())
            .subscribe(
                () -> jobFinished(params, false),
                rescheduleJobIfNetworkOrRedditError(params)
            )
    );
  }

  private void markAllMessagesAsRead(JobParameters params) {
    unsubscribeOnDestroy(
        Dank.reddit().withAuth(Completable.fromAction(() -> Dank.reddit().userInboxManager().setAllRead()))
            .andThen(Dank.messagesNotifManager().removeAllMessageNotifSeenStatuses())
            .compose(applySchedulersCompletable())
            .subscribe(
                () -> jobFinished(params, false),
                rescheduleJobIfNetworkOrRedditError(params)
            )
    );
  }

  private Consumer<Throwable> rescheduleJobIfNetworkOrRedditError(JobParameters params) {
    return error -> {
      ResolvedError resolvedError = Dank.errors().resolve(error);
      if (resolvedError.isUnknown()) {
        Timber.e(error, "Unknown error. Job action: %s", params.getExtras().get(KEY_ACTION));
      }

      Timber.i("Rescheduling job %s", params.getExtras().get(KEY_ACTION));
      boolean needsReschedule = resolvedError.isNetworkError() || resolvedError.isRedditServerError();
      jobFinished(params, needsReschedule);
    };
  }

  @Override
  public boolean onStopJob(JobParameters params) {
    // Return true to indicate JobScheduler that the job should be rescheduled.
    return false;
  }

  private Message parseMessage(String messageJson) {
    return JrawUtils.parseMessageJson(messageJson, Dank.jackson());
  }

}
