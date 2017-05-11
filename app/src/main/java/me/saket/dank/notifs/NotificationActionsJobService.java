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
import me.saket.dank.DankJobService;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.utils.JacksonHelper;
import me.saket.dank.utils.JrawUtils;
import timber.log.Timber;

public class NotificationActionsJobService extends DankJobService {

  private static final String KEY_MESSAGE_JSON = "message";
  private static final String KEY_MESSAGE_DIRECT_REPLY = "messageDirectReply";

  /**
   * We use JobScheduler for marking a message as read so that it handles retrying the
   * job when there's no network or any other error occurs.
   */
  public static void markAsRead(Context context, Message message, JacksonHelper jacksonHelper) {
    PersistableBundle extras = new PersistableBundle(1);
    extras.putString(KEY_MESSAGE_JSON, jacksonHelper.toJson(message));

    // Each job needs a unique ID and since we create a separate job for each message.
    int jobId = ID_MARK_MESSAGE_AS_READ + message.getId().hashCode();

    JobInfo markAsReadJob = new JobInfo.Builder(jobId, new ComponentName(context, NotificationActionsJobService.class))
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

  public static void sendDirectReply(Context context, Message replyToMessage, JacksonHelper jacksonHelper, String replyText) {
    PersistableBundle extras = new PersistableBundle(2);
    extras.putString(KEY_MESSAGE_JSON, jacksonHelper.toJson(replyToMessage));
    extras.putString(KEY_MESSAGE_DIRECT_REPLY, replyText);

    // Each job needs a unique ID and since we create a separate job for each message.
    int jobId = ID_SEND_DIRECT_MESSAGE_REPLY + replyToMessage.getId().hashCode();

    JobInfo markAsReadJob = new JobInfo.Builder(jobId, new ComponentName(context, NotificationActionsJobService.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPersisted(true)
        .setExtras(extras)
        .setOverrideDeadline(0)
        .build();

    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    jobScheduler.schedule(markAsReadJob);
  }

  @Override
  public boolean onStartJob(JobParameters params) {
    String messageJson = params.getExtras().getString(KEY_MESSAGE_JSON);
    Message message = JrawUtils.parseMessageJson(messageJson, Dank.jackson());

    switch (params.getJobId()) {
      case ID_MARK_MESSAGE_AS_READ:
        markMessageAsRead(params, message);
        break;

      case ID_SEND_DIRECT_MESSAGE_REPLY:
        String replyText = params.getExtras().getString(KEY_MESSAGE_DIRECT_REPLY);
        sendDirectMessageReply(params, message, replyText);
    }

    // Return true to indicate that the job is still being processed (in a background thread).
    return true;
  }

  private void markMessageAsRead(JobParameters params, Message message) {
    unsubscribeOnDestroy(
        Dank.reddit().withAuth(Completable.fromAction(() -> Dank.reddit().userInboxManager().setRead(true, message)))
            .compose(applySchedulersCompletable())
            .subscribe(() -> {
              jobFinished(params, false /* needsReschedule */);

            }, error -> {
              ResolvedError resolvedError = Dank.errors().resolve(error);
              if (resolvedError.isUnknown()) {
                Timber.e(error, "Unknown error while marking message as read.");
              }

              boolean needsReschedule = resolvedError.isNetworkError() || resolvedError.isRedditServerError();
              jobFinished(params, needsReschedule);
            })
    );
  }

  private void sendDirectMessageReply(JobParameters params, Message replyToMessage, String replyText) {
    unsubscribeOnDestroy(
        Dank.reddit().withAuth(Completable.fromAction(() -> Dank.reddit().userAccountManager().reply(replyToMessage, replyText)))
        .compose(applySchedulersCompletable())
        .subscribe(() -> {
          jobFinished(params, false /* needsReschedule */);

        }, error -> {
          ResolvedError resolvedError = Dank.errors().resolve(error);
          if (resolvedError.isUnknown()) {
            Timber.e(error, "Unknown error while sending direct reply to a message.");
          }

          boolean needsReschedule = resolvedError.isNetworkError() || resolvedError.isRedditServerError();
          jobFinished(params, needsReschedule);
        })
    );
  }

  @Override
  public boolean onStopJob(JobParameters params) {
    // Return true to indicate JobScheduler that the job should be rescheduled.
    return false;
  }

}
