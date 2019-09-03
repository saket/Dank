package me.saket.dank.utils.lifecycle;

import android.content.Intent;

import androidx.annotation.CheckResult;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import me.saket.dank.data.ActivityResult;

public class ActivityLifecycleStreams implements LifecycleStreams<ActivityLifecycleEvent> {

  protected static final Object INSTANCE = new Object();

  private PublishSubject<ActivityLifecycleEvent> events = PublishSubject.create();
  private PublishSubject<ActivityResult> onActivityResults = PublishSubject.create();

  protected void accept(ActivityLifecycleEvent event) {
    events.onNext(event);
  }

  public void notifyOnActivityResult(int requestCode, int resultCode, Intent data) {
    onActivityResults.onNext(ActivityResult.create(requestCode, resultCode, data));
  }

  @Override
  public Observable<ActivityLifecycleEvent> events() {
    return events;
  }

  @Override
  @CheckResult
  public Observable<ActivityLifecycleEvent> onStart() {
    return events.filter(e -> e == ActivityLifecycleEvent.START);
  }

  @Override
  @CheckResult
  public Observable<ActivityLifecycleEvent> onResume() {
    return events.filter(e -> e == ActivityLifecycleEvent.RESUME);
  }

  @Override
  @CheckResult
  public Observable<ActivityLifecycleEvent> onPause() {
    return events.filter(e -> e == ActivityLifecycleEvent.PAUSE);
  }

  @Override
  public Flowable<ActivityLifecycleEvent> onStopFlowable() {
    return onStop().toFlowable(BackpressureStrategy.LATEST);
  }

  @CheckResult
  public Observable<ActivityLifecycleEvent> onStop() {
    return events.filter(e -> e == ActivityLifecycleEvent.STOP);
  }

  @Override
  @CheckResult
  public Observable<ActivityLifecycleEvent> onDestroy() {
    return events.filter(e -> e == ActivityLifecycleEvent.DESTROY).take(1);
  }

  @CheckResult
  public Observable<ActivityResult> onActivityResults() {
    return onActivityResults;
  }
}
