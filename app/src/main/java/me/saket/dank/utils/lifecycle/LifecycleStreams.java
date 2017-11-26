package me.saket.dank.utils.lifecycle;

import android.support.annotation.CheckResult;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import me.saket.dank.data.ActivityResult;

/**
 * This is an interface so that subclasses don't forget to expose all the streams, especially delegating subclasses.
 */
public interface LifecycleStreams {

  Object NOTHING = new Object();

  // Commented out because dialog fragments have two create methods: onCreate() and onCreateDialog().
  /*public Observable<Object> onCreate() {
    return onCreates;
  }*/

  @CheckResult
  Observable<Object> onStart();

  @CheckResult
  Observable<Object> onResume();

  @CheckResult
  Observable<Object> onPause();

  @CheckResult
  Observable<Object> onStop();

  @CheckResult
  Observable<Object> onDestroy();

  @CheckResult
  Flowable<Object> onDestroyFlowable();

  @CheckResult
  Observable<ActivityResult> onActivityResults();
}
