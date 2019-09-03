package me.saket.dank.ui.subscriptions;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.text.format.DateUtils;

import com.jakewharton.rxrelay2.BehaviorRelay;

import javax.inject.Inject;

import me.saket.dank.BuildConfig;
import me.saket.dank.DankJobService;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import timber.log.Timber;

import static me.saket.dank.utils.RxUtils.applySchedulersCompletable;
import static me.saket.dank.utils.RxUtils.doOnCompletableStartAndTerminate;

/**
 * Syncs user's subreddit subscriptions in background. This brings in user's new subscriptions + executes
 * any pending subscribe or unsubscribe actions.
 */
public class SubredditSubscriptionsSyncJob extends DankJobService {

  /**
   * Emits true when bookmarks are being fetched. False when the process completes.
   * (regardless of the outcome).
   */
  private static final BehaviorRelay<Boolean> progressSubject = BehaviorRelay.create();

  @Inject SubscriptionRepository subscriptionRepository;

  /**
   * Sync subscriptions every ~6 hours when the device is idle, charging and on an unmetered connection.
   */
  public static void schedule(Context context) {
    JobInfo syncJob = new JobInfo.Builder(ID_SUBSCRIPTIONS_RECURRING_JOB, new ComponentName(context, SubredditSubscriptionsSyncJob.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
        .setRequiresCharging(true)
        .setRequiresDeviceIdle(true)
        .setPersisted(true)
        .setPeriodic(DateUtils.HOUR_IN_MILLIS * 6)
        .build();

    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    //noinspection ConstantConditions
    jobScheduler.schedule(syncJob);
  }

  public static void syncImmediately(Context context) {
    JobInfo syncJob = new JobInfo.Builder(ID_SUBSCRIPTIONS_ONE_TIME_JOB, new ComponentName(context, SubredditSubscriptionsSyncJob.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPersisted(false)
        .setOverrideDeadline(0)
        .build();

    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    //noinspection ConstantConditions
    jobScheduler.schedule(syncJob);
  }

  /**
   * See {@link #progressSubject}.
   */
  public static BehaviorRelay<Boolean> progressUpdates() {
    return progressSubject;
  }

  @Override
  public void onCreate() {
    Dank.dependencyInjector().inject(this);
    super.onCreate();
  }

  @Override
  public JobStartCallback onStartJob2(JobParameters params) {
    subscriptionRepository.refreshAndSaveSubscriptions()
        .andThen(subscriptionRepository.executePendingSubscribesAndUnsubscribes())
        .compose(applySchedulersCompletable())
        .compose(doOnCompletableStartAndTerminate(ongoing -> progressSubject.accept(ongoing)))
        .ambWith(lifecycleOnDestroy().ignoreElements())
        .subscribe(
            () -> {
              if (params.getJobId() == ID_SUBSCRIPTIONS_RECURRING_JOB && BuildConfig.DEBUG) {
                displayDebugNotification("Subreddits synced");
              }
              jobFinished(params, false);
            },
            error -> {
              ResolvedError resolvedError = Dank.errors().resolve(error);
              if (resolvedError.isUnknown()) {
                Timber.e(error, "Unknown error while syncing subscriptions");
              }

              boolean isNotOurFault = resolvedError.isNetworkError() || resolvedError.isRedditServerError();
              boolean canRetry = isNotOurFault && params.getJobId() == ID_SUBSCRIPTIONS_RECURRING_JOB;
              jobFinished(params, canRetry);
            }
        );

    return JobStartCallback.runningInBackground();
  }

  @Override
  public JobStopCallback onStopJob2() {
    return JobStopCallback.rescheduleRequired();
  }
}
