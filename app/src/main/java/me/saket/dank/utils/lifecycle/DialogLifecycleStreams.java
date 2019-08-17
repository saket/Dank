package me.saket.dank.utils.lifecycle;

import androidx.annotation.CheckResult;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class DialogLifecycleStreams implements LifecycleStreams<DialogLifecycleEvent> {

  private PublishSubject<DialogLifecycleEvent> events = PublishSubject.create();

  protected void accept(DialogLifecycleEvent event) {
    events.onNext(event);
  }

  @Override
  public Observable<DialogLifecycleEvent> events() {
    return events;
  }

  @CheckResult
  public Observable<DialogLifecycleEvent> onStart() {
    return events.filter(e -> e == DialogLifecycleEvent.START);
  }

  @CheckResult
  public Observable<DialogLifecycleEvent> onResume() {
    return events.filter(e -> e == DialogLifecycleEvent.RESUME);
  }

  @CheckResult
  public Observable<DialogLifecycleEvent> onPause() {
    return events.filter(e -> e == DialogLifecycleEvent.PAUSE);
  }

  @CheckResult
  public Observable<DialogLifecycleEvent> onStop() {
    return events.filter(e -> e == DialogLifecycleEvent.STOP);
  }

  @CheckResult
  public Observable<DialogLifecycleEvent> onDestroy() {
    return events.filter(e -> e == DialogLifecycleEvent.DESTROY).take(1);
  }
}
