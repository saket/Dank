package me.saket.dank.utils.lifecycle;

import android.content.Intent;

import androidx.annotation.CheckResult;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import me.saket.dank.data.ActivityResult;

public class FragmentLifecycleStreams implements LifecycleStreams<FragmentLifecycleEvent> {

  private PublishSubject<FragmentLifecycleEvent> events = PublishSubject.create();
  private PublishSubject<ActivityResult> onActivityResults = PublishSubject.create();

  protected void accept(FragmentLifecycleEvent event) {
    events.onNext(event);
  }

  public void notifyOnActivityResult(int requestCode, int resultCode, Intent data) {
    onActivityResults.onNext(ActivityResult.create(requestCode, resultCode, data));
  }

  @Override
  public Observable<FragmentLifecycleEvent> events() {
    return events;
  }

  @Override
  @CheckResult
  public Observable<FragmentLifecycleEvent> onStart() {
    return events.filter(e -> e == FragmentLifecycleEvent.START);
  }

  @Override
  @CheckResult
  public Observable<FragmentLifecycleEvent> onResume() {
    return events.filter(e -> e == FragmentLifecycleEvent.RESUME);
  }

  @Override
  @CheckResult
  public Observable<FragmentLifecycleEvent> onPause() {
    return events.filter(e -> e == FragmentLifecycleEvent.PAUSE);
  }

  @Override
  @CheckResult
  public Observable<FragmentLifecycleEvent> onStop() {
    return events.filter(e -> e == FragmentLifecycleEvent.STOP);
  }

  @Override
  @CheckResult
  public Observable<FragmentLifecycleEvent> onDestroy() {
    return events.filter(e -> e == FragmentLifecycleEvent.DESTROY).take(1);
  }

  @CheckResult
  public Observable<ActivityResult> onActivityResults() {
    return onActivityResults;
  }
}
