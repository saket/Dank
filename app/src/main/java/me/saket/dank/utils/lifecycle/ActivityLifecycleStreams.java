package me.saket.dank.utils.lifecycle;

import android.content.Intent;
import android.support.annotation.CheckResult;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import me.saket.dank.data.ActivityResult;

public class ActivityLifecycleStreams implements LifecycleStreams {

  protected static final Object INSTANCE = new Object();

  private Subject<Object> onCreates = PublishSubject.create();
  private Subject<Object> onStarts = PublishSubject.create();
  private Subject<Object> onResumes = PublishSubject.create();
  private Subject<Object> onPauses = PublishSubject.create();
  private Subject<Object> onStops = PublishSubject.create();
  private Subject<Object> onDestroys = PublishSubject.create();
  private Subject<ActivityResult> onActivityResults = PublishSubject.create();

  protected void notifyOnCreate() {
    onCreates.onNext(INSTANCE);
  }

  protected void notifyOnStart() {
    onStarts.onNext(INSTANCE);
  }

  protected void notifyOnResume() {
    onResumes.onNext(INSTANCE);
  }

  protected void notifyOnPause() {
    onPauses.onNext(INSTANCE);
  }

  protected void notifyOnStop() {
    onStops.onNext(INSTANCE);
  }

  protected void notifyOnDestroy() {
    onDestroys.onNext(INSTANCE);
  }

  public void notifyOnActivityResult(int requestCode, int resultCode, Intent data) {
    onActivityResults.onNext(ActivityResult.create(requestCode, resultCode, data));
  }

  // Commented out because dialog fragments have two create methods: onCreate() and onCreateDialog().
  /*public Observable<Object> onCreate() {
    return onCreates;
  }*/

  @CheckResult
  public Observable<Object> onStart() {
    return onStarts;
  }

  @CheckResult
  public Observable<Object> onResume() {
    return onResumes;
  }

  @CheckResult
  public Observable<Object> onPause() {
    return onPauses;
  }

  @CheckResult
  public Observable<Object> onStop() {
    return onStops;
  }

  @CheckResult
  public Observable<Object> onDestroy() {
    return onDestroys;
  }

  @CheckResult
  public Flowable<Object> onDestroyFlowable() {
    return onDestroy().toFlowable(BackpressureStrategy.LATEST);
  }

  @CheckResult
  public Observable<ActivityResult> onActivityResults() {
    return onActivityResults;
  }
}
