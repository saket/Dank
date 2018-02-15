package me.saket.dank.utils.lifecycle;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

public class LifecycleOwnerDialogFragment extends DialogFragment implements LifecycleOwner<DialogLifecycleEvent> {

  private DialogLifecycleStreams lifecycleStreams;

  @Override
  public DialogLifecycleStreams lifecycle() {
    return lifecycleStreams;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    lifecycleStreams = new DialogLifecycleStreams();
  }

  @Override
  public void onStart() {
    super.onStart();
    lifecycleStreams.accept(DialogLifecycleEvent.START);
  }

  @Override
  public void onResume() {
    super.onResume();
    lifecycleStreams.accept(DialogLifecycleEvent.RESUME);
  }

  @Override
  public void onPause() {
    lifecycleStreams.accept(DialogLifecycleEvent.PAUSE);
    super.onPause();
  }

  @Override
  public void onStop() {
    lifecycleStreams.accept(DialogLifecycleEvent.STOP);
    super.onStop();
  }

  @Override
  public void onDestroy() {
    lifecycleStreams.accept(DialogLifecycleEvent.DESTROY);
    super.onDestroy();
  }
}
