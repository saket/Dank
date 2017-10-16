package me.saket.dank.utils.lifecycle;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import com.trello.navi2.component.support.NaviDialogFragment;
import timber.log.Timber;

public class LifecycleOwnerDialogFragment extends NaviDialogFragment implements LifecycleOwner {

  private LifecycleStreams lifecycleStreams;

  @Override
  public LifecycleStreams lifecycle() {
    return lifecycleStreams;
  }

  @NonNull
  @Override
  @CallSuper
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    lifecycleStreams = new LifecycleStreams();
    lifecycleStreams.notifyOnCreate();
    return super.onCreateDialog(savedInstanceState);
  }

  @Override
  public void onStart() {
    Timber.i("onStart()");
    super.onStart();
    lifecycleStreams.notifyOnStart();
  }

  @Override
  public void onResume() {
    super.onResume();
    lifecycleStreams.notifyOnResume();
  }

  @Override
  public void onPause() {
    lifecycleStreams.notifyOnPause();
    super.onPause();
  }

  @Override
  public void onStop() {
    lifecycleStreams.notifyOnStop();
    super.onStop();
  }

  @Override
  public void onDestroy() {
    lifecycleStreams.notifyOnDestroy();
    super.onDestroy();
  }
}
