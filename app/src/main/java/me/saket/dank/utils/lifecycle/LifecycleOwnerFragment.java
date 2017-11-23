package me.saket.dank.utils.lifecycle;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class LifecycleOwnerFragment extends Fragment implements LifecycleOwner {

  private LifecycleStreams lifecycleStreams;

  @Override
  public LifecycleStreams lifecycle() {
    return lifecycleStreams;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    lifecycleStreams = new LifecycleStreams();
    lifecycleStreams.notifyOnCreate();
  }

  @Override
  public void onStart() {
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
