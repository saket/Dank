package me.saket.dank.notifs;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;

import androidx.annotation.CheckResult;

import net.dean.jraw.models.Message;

import java.util.Arrays;

import javax.inject.Inject;

import dagger.Lazy;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import me.saket.dank.DankJobService;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.InboxRepository;
import me.saket.dank.data.MoshiAdapter;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.reddit.Reddit;
import me.saket.dank.utils.JrawUtils2;
import timber.log.Timber;

import static me.saket.dank.utils.RxUtils.applySchedulersCompletable;

/**
 * We're using JobScheduler for executing notification actions so that it also handles auto-retrying when
 * there's no network or reddit's servers are down.
 */
public class MessageNotifActionsJobService extends DankJobService {

  private static final String KEY_MESSAGE_JSON = "messageJson";
  private static final String KEY_MESSAGE_ARRAY_JSON = "messageArrayJson";
  private static final String KEY_MESSAGE_DIRECT_REPLY = "messageDirectReply";
  private static final String KEY_ACTION = "action";

  private static final String ACTION_SEND_DIRECT_REPLY = "sendDirectReply";
  private static final String ACTION_MARK_MESSAGE_AS_READ = "markMessageAsRead";
  private static final String ACTION_MARK_ALL_MESSAGES_AS_READ = "markAllMessagesAsRead";

  @Inject InboxRepository inboxRepository;
  @Inject ErrorResolver errorResolver;
  @Inject Lazy<MoshiAdapter> moshiAdapter;
  @Inject MessagesNotificationManager messagesNotifManager;
  @Inject Lazy<Reddit> reddit;

  public static void sendDirectReply(Context context, Message replyToMessage, MoshiAdapter moshiAdapter, String replyText) {
    PersistableBundle extras = new PersistableBundle(3);
    extras.putString(KEY_MESSAGE_JSON, moshiAdapter.create(Message.class).toJson(replyToMessage));
    extras.putString(KEY_MESSAGE_DIRECT_REPLY, replyText);
    extras.putString(KEY_ACTION, ACTION_SEND_DIRECT_REPLY);

    // Each job needs a unique ID and since we create a separate job for each message.
    int jobId = (int) (ID_SEND_DIRECT_MESSAGE_REPLY + JrawUtils2.generateAdapterId(replyToMessage));

    JobInfo markAsReadJob = new JobInfo.Builder(jobId, new ComponentName(context, MessageNotifActionsJobService.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPersisted(true)
        .setExtras(extras)
        .setOverrideDeadline(0)
        .build();
    // JobInfo use a default back-off criteria of (30s, exponential) so we're not setting
    // it ourselves.

    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    //noinspection ConstantConditions
    jobScheduler.schedule(markAsReadJob);
  }

  public static void markAsRead(Context context, MoshiAdapter moshiAdapter, Message... messages) {
    PersistableBundle extras = new PersistableBundle(2);
    extras.putString(KEY_ACTION, ACTION_MARK_MESSAGE_AS_READ);
    extras.putString(KEY_MESSAGE_ARRAY_JSON, moshiAdapter.create(Message[].class).toJson(messages));

    int jobId = ID_MARK_MESSAGE_AS_READ + Arrays.hashCode(messages);

    JobInfo markAsReadJob = new JobInfo.Builder(jobId, new ComponentName(context, MessageNotifActionsJobService.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPersisted(true)
        .setExtras(extras)
        .setOverrideDeadline(0)
        .build();

    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    //noinspection ConstantConditions
    jobScheduler.schedule(markAsReadJob);
  }

  public static void markAllAsRead(Context context) {
    PersistableBundle extras = new PersistableBundle(1);
    extras.putString(KEY_ACTION, ACTION_MARK_ALL_MESSAGES_AS_READ);

    JobInfo markAsReadJob = new JobInfo.Builder(ID_MARK_ALL_MESSAGES_AS_READ, new ComponentName(context, MessageNotifActionsJobService.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPersisted(true)
        .setExtras(extras)
        .setOverrideDeadline(0)
        .build();
    // JobInfo use a default back-off criteria of (30s, exponential) so we're not setting
    // it ourselves.

    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    //noinspection ConstantConditions
    jobScheduler.schedule(markAsReadJob);
  }

  @Override
  public void onCreate() {
    Dank.dependencyInjector().inject(this);
    super.onCreate();
  }

  @Override
  public JobStartCallback onStartJob2(JobParameters params) {
    // Switching on the action because a new job is created for each message, each with an unique jobId.
    //noinspection ConstantConditions
    switch (params.getExtras().getString(KEY_ACTION)) {
      case ACTION_SEND_DIRECT_REPLY:
        String replyText = params.getExtras().getString(KEY_MESSAGE_DIRECT_REPLY);
        //noinspection ConstantConditions
        sendDirectMessageReply(params, replyText);
        break;

      case ACTION_MARK_MESSAGE_AS_READ:
        markMessageAsRead(params);
        break;

      case ACTION_MARK_ALL_MESSAGES_AS_READ:
        markAllMessagesAsRead(params);
        break;

      default:
        throw new UnsupportedOperationException("Unknown job action: " + params.getExtras().getString(KEY_ACTION));
    }

    return JobStartCallback.runningInBackground();
  }

  private void sendDirectMessageReply(JobParameters params, String replyText) {
    //noinspection ConstantConditions
    unsubscribeOnDestroy(
        parseMessage(params.getExtras().getString(KEY_MESSAGE_JSON))
            .flatMapCompletable(replyToMessage -> reddit.get()
                .loggedInUser().reply(replyToMessage, replyText)
                .toCompletable()
                .andThen(messagesNotifManager.removeMessageNotifSeenStatus(replyToMessage)))
            .compose(applySchedulersCompletable())
            .subscribe(
                () -> jobFinished(params, false),
                rescheduleJobIfNetworkOrRedditError(params)
            )
    );
  }

  private void markMessageAsRead(JobParameters params) {
    //noinspection ConstantConditions
    unsubscribeOnDestroy(
        parseMessageArray(params.getExtras().getString(KEY_MESSAGE_ARRAY_JSON))
            .flatMapCompletable(messages -> inboxRepository.setRead(messages, true)
                .andThen(messagesNotifManager.removeMessageNotifSeenStatus(messages)))
            .compose(applySchedulersCompletable())
            .subscribe(
                () -> jobFinished(params, false),
                rescheduleJobIfNetworkOrRedditError(params)
            ));
  }

  private void markAllMessagesAsRead(JobParameters params) {
    unsubscribeOnDestroy(
        inboxRepository.setAllRead()
            .andThen(messagesNotifManager.removeAllMessageNotifSeenStatuses())
            .compose(applySchedulersCompletable())
            .subscribe(
                () -> jobFinished(params, false),
                rescheduleJobIfNetworkOrRedditError(params)
            )
    );
  }

  private Consumer<Throwable> rescheduleJobIfNetworkOrRedditError(JobParameters params) {
    return error -> {
      ResolvedError resolvedError = errorResolver.resolve(error);
      if (resolvedError.isUnknown()) {
        Timber.e(error, "Unknown error. Job action: %s", params.getExtras().get(KEY_ACTION));
      }

      Timber.i("Rescheduling job %s", params.getExtras().get(KEY_ACTION));
      boolean needsReschedule = resolvedError.isNetworkError() || resolvedError.isRedditServerError();
      jobFinished(params, needsReschedule);
    };
  }

  @Override
  public JobStopCallback onStopJob2() {
    return JobStopCallback.rescheduleRequired();
  }

  @CheckResult
  private Single<Message> parseMessage(String messageJson) {
    return Single.fromCallable(() -> moshiAdapter.get().create(Message.class).fromJson(messageJson));
  }

  @CheckResult
  private Single<Message[]> parseMessageArray(String messageArrayJson) {
    return Single.fromCallable(() -> moshiAdapter.get().create(Message[].class).fromJson(messageArrayJson));
  }

}
