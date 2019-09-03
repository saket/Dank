package me.saket.dank.cache;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.schedulers.Schedulers;
import me.saket.dank.BuildConfig;
import me.saket.dank.DankJobService;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.submission.SubmissionRepository;
import timber.log.Timber;

/**
 * Runs every day, recycles DB rows older than 30 days (1 for debug variants).
 */
public class DatabaseCacheRecyclerJobService extends DankJobService {

  @Inject SubmissionRepository submissionRepository;

  public static void schedule(Context context) {
    JobInfo.Builder builder = new JobInfo.Builder(ID_RECYCLE_OLD_SUBMISSIONS, new ComponentName(context, DatabaseCacheRecyclerJobService.class))
        .setRequiresDeviceIdle(true)
        .setRequiresCharging(true)
        .setPersisted(true)
        .setPeriodic(TimeUnit.DAYS.toMillis(1));

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      builder = builder.setRequiresBatteryNotLow(true);
    }

    Timber.i("Scheduling recycling service");

    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    //noinspection ConstantConditions
    jobScheduler.schedule(builder.build());
  }

  @Override
  public void onCreate() {
    Dank.dependencyInjector().inject(this);
    super.onCreate();
  }

  @Override
  public JobStartCallback onStartJob2(JobParameters params) {
    int durationFromNow = BuildConfig.DEBUG ? 0 : 5;
    TimeUnit durationTimeUnit = TimeUnit.DAYS;

    displayDebugNotification(
        ID_DEBUG_RECYCLING,
        "Recycling database rows older than %s day(s)",
        durationTimeUnit.toDays(durationFromNow));

    submissionRepository.recycleAllCachedBefore(durationFromNow, durationTimeUnit)
        .subscribeOn(Schedulers.io())
        .takeUntil(lifecycleOnDestroy().ignoreElements())
        .subscribe(
            deletedRows -> {
              displayDebugNotification(
                  ID_DEBUG_RECYCLING,
                  "Recycled %s database rows older than %s day(s)",
                  deletedRows,
                  durationTimeUnit.toDays(durationFromNow));
            },
            error -> {
              Timber.e(error, "Couldn't recycle database rows");
              displayDebugNotification(
                  ID_DEBUG_RECYCLING,
                  "Couldn't recycle database rows");
            }
        );

    return JobStartCallback.runningInBackground();
  }

  @Override
  public JobStopCallback onStopJob2() {
    return JobStopCallback.drop();
  }
}
