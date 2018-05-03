package me.saket.dank.ui;

import android.content.Context;
import android.support.annotation.Nullable;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import me.saket.dank.utils.lifecycle.LifecycleOwnerFragment;

/**
 * Base class for fragments.
 */
public class DankFragment extends LifecycleOwnerFragment {

  private CompositeDisposable onStopDisposables;
  private CompositeDisposable onDestroyDisposables;

  @Nullable
  @Override
  public Context getContext() {
    throw new AssertionError("Use requireContext() instead");
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
