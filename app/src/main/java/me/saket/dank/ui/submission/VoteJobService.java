package me.saket.dank.ui.submission;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.models.VoteDirection;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.DankJobService;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.data.VotableContributionFullNameWrapper;
import me.saket.dank.data.VotingManager;
import me.saket.dank.di.Dank;
import timber.log.Timber;

/**
 * Used for re-trying failed vote attempts.
 */
public class VoteJobService extends DankJobService {

  private static final String KEY_VOTABLE_CONTRIBUTION_JSON = "votableThingName";
  private static final String KEY_VOTE_DIRECTION = "voteDirection";

  @Inject Moshi moshi;
  @Inject VotingManager votingManager;

  /**
   * Schedule a voting attempt whenever JobScheduler deems it fit.
   */
  public static void scheduleRetry(Context context, PublicContribution votableContribution, VoteDirection voteDirection, Moshi moshi) {
    PersistableBundle extras = new PersistableBundle(2);
    extras.putString(KEY_VOTE_DIRECTION, voteDirection.name());

    VotableContributionFullNameWrapper fauxVotableContribution = VotableContributionFullNameWrapper.createFrom(votableContribution);
    JsonAdapter<VotableContributionFullNameWrapper> jsonAdapter = moshi.adapter(VotableContributionFullNameWrapper.class);
    String contributionJson = jsonAdapter.toJson(fauxVotableContribution);
    extras.putString(KEY_VOTABLE_CONTRIBUTION_JSON, contributionJson);

    JobInfo retryJobInfo = new JobInfo.Builder(ID_VOTE + votableContribution.hashCode(), new ComponentName(context, VoteJobService.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPersisted(true)
        .setExtras(extras)
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
  public JobStartCallback onStartJob2(JobParameters params) {
    VoteDirection voteDirection = VoteDirection.valueOf(params.getExtras().getString(KEY_VOTE_DIRECTION));

    JsonAdapter<VotableContributionFullNameWrapper> jsonAdapter = moshi.adapter(VotableContributionFullNameWrapper.class);
    String votableContributionJson = params.getExtras().getString(KEY_VOTABLE_CONTRIBUTION_JSON);

    //noinspection ConstantConditions
    Single.fromCallable(() -> jsonAdapter.fromJson(votableContributionJson))
        .flatMapCompletable(votableContribution -> {
          if (!votingManager.isVotePending(votableContribution)) {
            // Looks like the pending vote was cleared upon refreshing data from remote.
            Timber.w("Job is stale because contribution no longer has a pending vote: %s", votableContribution);
            return Completable.complete();

          } else {
            return votingManager.vote(votableContribution, voteDirection);
          }
        })
        .ambWith(lifecycleOnDestroy().ignoreElements())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
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
        );

    return JobStartCallback.runningInBackground();
  }

  @Override
  public boolean onStopJob(JobParameters params) {
    // Return true to indicate JobScheduler that the job should be rescheduled.
    return true;
  }
}
