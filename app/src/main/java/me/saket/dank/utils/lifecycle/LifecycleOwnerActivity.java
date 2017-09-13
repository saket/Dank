package me.saket.dank.utils.lifecycle;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

public abstract class LifecycleOwnerActivity extends AppCompatActivity implements LifecycleOwner {

  private LifecycleStreams lifecycleStreams;

  @Override
  public LifecycleStreams lifecycle() {
    return lifecycleStreams;
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    lifecycleStreams = new LifecycleStreams();
    lifecycleStreams.notifyOnCreate();
  }

  @Override
  protected void onStart() {
    super.onStart();
    lifecycleStreams.notifyOnStart();
  }

  @Override
  protected void onResume() {
    super.onResume();
    lifecycleStreams.notifyOnResume();
  }

  @Override
  protected void onPause() {
    lifecycleStreams.notifyOnPause();
    super.onPause();
  }

  @Override
  protected void onStop() {
    lifecycleStreams.notifyOnStop();
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    lifecycleStreams.notifyOnDestroy();
    super.onDestroy();
  }
}
