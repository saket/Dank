package me.saket.dank.utils.lifecycle;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import me.saket.dank.data.ActivityResult;

public abstract class ForwardingLifecycleStreams implements LifecycleStreams {

  private final LifecycleStreams delegate;

  public ForwardingLifecycleStreams(LifecycleStreams delegate) {
    this.delegate = delegate;
  }

  @Override
  public Observable<Object> onStart() {
    return delegate.onStart();
  }

  @Override
  public Observable<Object> onResume() {
    return delegate.onResume();
  }

  @Override
  public Observable<Object> onPause() {
    return delegate.onPause();
  }

  @Override
  public Observable<Object> onStop() {
    return delegate.onStop();
  }

  @Override
  public Observable<Object> onDestroy() {
    return delegate.onDestroy();
  }

  @Override
  public Flowable<Object> onDestroyFlowable() {
    return delegate.onDestroyFlowable();
  }

  @Override
  public Observable<ActivityResult> onActivityResults() {
    return delegate.onActivityResults();
  }
}
