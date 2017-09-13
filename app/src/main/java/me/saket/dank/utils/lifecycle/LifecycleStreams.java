package me.saket.dank.utils.lifecycle;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class LifecycleStreams {

  private static final Object INSTANCE = new Object();

  private Subject<Object> onCreates = PublishSubject.create();
  private Subject<Object> onStarts = PublishSubject.create();
  private Subject<Object> onResumes = PublishSubject.create();
  private Subject<Object> onPauses = PublishSubject.create();
  private Subject<Object> onStops = PublishSubject.create();
  private Subject<Object> onDestroys = PublishSubject.create();

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

  public Observable<Object> onCreate() {
    return onCreates;
  }

  public Observable<Object> onStart() {
    return onStarts;
  }

  public Observable<Object> onResume() {
    return onResumes;
  }

  public Observable<Object> onPause() {
    return onPauses;
  }

  public Observable<Object> onStop() {
    return onStops;
  }

  public Observable<Object> onDestroy() {
    return onDestroys;
  }
}
