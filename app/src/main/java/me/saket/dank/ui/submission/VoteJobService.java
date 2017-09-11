package me.saket.dank.ui.submission;

import static me.saket.dank.utils.RxUtils.applySchedulersCompletable;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;

import net.dean.jraw.models.Thing;
import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.models.attr.Votable;

import me.saket.dank.DankJobService;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import timber.log.Timber;

/**
 * Used for re-trying failed vote attempts.
 */
public class VoteJobService extends DankJobService {

  private static final String KEY_VOTABLE_THING_NAME = "votableThingName";
  private static final String KEY_VOTE_DIRECTION = "voteDirection";

  /**
   * Schedule a voting attempt whenever JobScheduler deems it fit.
   */
  public static <T extends Thing & Votable> void scheduleRetry(Context context, T thing, VoteDirection voteDirection) {
    PersistableBundle extras = new PersistableBundle(2);
    extras.putString(KEY_VOTE_DIRECTION, voteDirection.name());
    extras.putString(KEY_VOTABLE_THING_NAME, thing.getFullName());

    JobInfo retryJobInfo = new JobInfo.Builder(ID_VOTE + thing.getId().hashCode(), new ComponentName(context, VoteJobService.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPersisted(true)
        .setExtras(extras)
        .build();

    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    jobScheduler.schedule(retryJobInfo);
  }

  @Override
  public boolean onStartJob(JobParameters params) {
    VoteDirection voteDirection = VoteDirection.valueOf(params.getExtras().getString(KEY_VOTE_DIRECTION));
    String fullNameOfThingToVote = params.getExtras().getString(KEY_VOTABLE_THING_NAME);

    if (!Dank.voting().isVotePending(fullNameOfThingToVote)) {
      // Looks like the pending vote was cleared upon refreshing data from remote.
      Timber.w("Job is stale because %s no longer has a pending vote.", fullNameOfThingToVote);
      jobFinished(params, false);
    }

    unsubscribeOnDestroy(Dank.voting().vote(fullNameOfThingToVote, voteDirection)
        .compose(applySchedulersCompletable())
        .subscribe(
            () -> {
              Timber.i("Finis");
              jobFinished(params, false);
            },
            error -> {
              ResolvedError resolvedError = Dank.errors().resolve(error);
              boolean needsReschedule = resolvedError.isNetworkError() || resolvedError.isRedditServerError();

              Timber.i("Retry failed: %s", error.getMessage());
              Timber.i("needsReschedule: %s", needsReschedule);
              jobFinished(params, needsReschedule);
            }
        ));

    // Return true to indicate that the job is still being processed (in a background thread).
    return true;
  }

  @Override
  public boolean onStopJob(JobParameters params) {
    // Return true to indicate JobScheduler that the job should be rescheduled.
    return true;
  }
}
