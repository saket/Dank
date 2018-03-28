package me.saket.dank.ui;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import me.saket.dank.utils.lifecycle.LifecycleOwnerFragment;

/**
 * Base class for fragments.
 */
public class DankFragment extends LifecycleOwnerFragment {

  private CompositeDisposable onStopDisposables;
  private CompositeDisposable onDestroyDisposables;

  // TODO: Remove once we update support library to v27.1.0.
  @NonNull
  public Activity requireActivity() {
    if (getActivity() == null) {
      throw new AssertionError();
    }
    return getActivity();
  }

  @NonNull
  public Object requireHost() {
    if (getHost() == null) {
      throw new AssertionError();
    }
    return getHost();
  }

  @NonNull
  public Context requireContext() {
    if (getContext() == null) {
      throw new AssertionError();
    }
    return getContext();
  }

  @Override
  public void onStop() {
    if (onStopDisposables != null) {
      onStopDisposables.clear();
    }
    super.onStop();
  }

  @Override
  public void onDestroy() {
    if (onDestroyDisposables != null) {
      onDestroyDisposables.clear();
    }
    super.onDestroy();
  }

  @Deprecated
  protected void unsubscribeOnStop(Disposable subscription) {
    if (onStopDisposables == null) {
      onStopDisposables = new CompositeDisposable();
    }
    onStopDisposables.add(subscription);
  }

  @Deprecated
  protected void unsubscribeOnDestroy(Disposable subscription) {
    if (onDestroyDisposables == null) {
      onDestroyDisposables = new CompositeDisposable();
    }
    onDestroyDisposables.add(subscription);
  }
}
