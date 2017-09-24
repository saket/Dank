package me.saket.dank;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.text.format.DateUtils;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import io.reactivex.schedulers.Schedulers;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.submission.SubmissionRepository;
import me.saket.dank.utils.RxUtils;

/**
 * Runs every day, recycles DB rows older than 30 days (1 for debug variants).
 */
public class DatabaseCacheRecyclerJobService extends DankJobService {

  @Inject SubmissionRepository submissionRepository;

  public static void schedule(Context context) {
    JobInfo recycleJobInfo = new JobInfo.Builder(ID_RECYCLE_OLD_SUBMISSIONS, new ComponentName(context, DatabaseCacheRecyclerJobService.class))
        .setRequiresDeviceIdle(true)
        .setRequiresCharging(true)
        .setPersisted(true)
        .setPeriodic(DateUtils.DAY_IN_MILLIS)
        .build();

    JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    //noinspection ConstantConditions
    jobScheduler.schedule(recycleJobInfo);
  }

  @Override
  public void onCreate() {
    Dank.dependencyInjector().inject(this);
    super.onCreate();
  }

  @Override
  public boolean onStartJob(JobParameters params) {
    int durationFromNow = BuildConfig.DEBUG ? 1 : 30;
    TimeUnit durationTimeUnit = TimeUnit.DAYS;

    submissionRepository.recycleAllCachedBefore(durationFromNow, durationTimeUnit)
        .subscribeOn(Schedulers.io())
        .subscribe(
            deletedRows -> {
              displayDebugNotification(
                  ID_RECYCLE_OLD_SUBMISSIONS,
                  "Recycled %s database rows older than %s days",
                  deletedRows, durationTimeUnit.toDays(durationFromNow)
              );
            },
            RxUtils.logError("Couldn't recycle old submissions")
        );

    // Return true to indicate that the job is still being processed (in a background thread).
    return false;
  }
}
