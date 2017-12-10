package me.saket.dank.ui.submission;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

import net.dean.jraw.ApiException;

import javax.inject.Inject;

import io.reactivex.Completable;
import me.saket.dank.DankJobService;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import timber.log.Timber;

/**
 * Retries sending of failed replies.
 */
public class RetryReplyJobService extends DankJobService {

  @Inject ReplyRepository replyRepository;

  public static void scheduleRetry(Context context) {
    JobInfo retryJobInfo = new JobInfo.Builder(ID_RETRY_REPLY, new ComponentName(context, RetryReplyJobService.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPersisted(true)
        .build();

    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    //noinspection ConstantConditions
    jobScheduler.schedule(retryJobInfo);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Dank.dependencyInjector().inject(this);
  }

  @Override
  public boolean onStartJob(JobParameters params) {
    unsubscribeOnDestroy(
        replyRepository.streamFailedReplies()
            .take(1)
            .flatMapIterable(failedReplies -> failedReplies)
            .flatMapCompletable(failedReply ->
                replyRepository.reSendReply(failedReply)
                    .onErrorResumeNext(error -> {
                      // A comment was made on an old submission. This shouldn't happen.
                      // Maybe our blocking of comments for old submission didn't work.
                      if (error instanceof ApiException && ((ApiException) error).getReason().contains("TOO_OLD")) {
                        return Completable.complete();
                      } else {
                        return Completable.error(error);
                      }
                    })
            )
            .subscribe(
                () -> jobFinished(params, false),
                error -> {
                  ResolvedError resolvedError = Dank.errors().resolve(error);
                  boolean canReschedule = resolvedError.isNetworkError() || resolvedError.isRedditServerError();

                  if (!canReschedule) {
                    Timber.e(error, "Failed to re-send message");
                  }
                  jobFinished(params, canReschedule);
                }
            )
    );

    // Return true to indicate that the job is still being processed (in a background thread).
    return true;
  }

  @Override
  public boolean onStopJob(JobParameters params) {
    // Return true to indicate JobScheduler that the job should be rescheduled.
    return true;
  }
}
