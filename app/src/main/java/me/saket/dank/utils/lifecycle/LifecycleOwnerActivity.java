package me.saket.dank.utils.lifecycle;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class LifecycleOwnerActivity extends AppCompatActivity implements LifecycleOwner<ActivityLifecycleEvent> {

  private ActivityLifecycleStreams lifecycleStreams;

  @Override
  public ActivityLifecycleStreams lifecycle() {
    return lifecycleStreams;
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    lifecycleStreams = new ActivityLifecycleStreams();
  }

  @Override
  protected void onStart() {
    super.onStart();
    lifecycleStreams.accept(ActivityLifecycleEvent.START);
  }

  @Override
  protected void onResume() {
    super.onResume();
    lifecycleStreams.accept(ActivityLifecycleEvent.RESUME);
  }

  @Override
  protected void onPause() {
    lifecycleStreams.accept(ActivityLifecycleEvent.PAUSE);
    super.onPause();
  }

  @Override
  protected void onStop() {
    lifecycleStreams.accept(ActivityLifecycleEvent.STOP);
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    lifecycleStreams.accept(ActivityLifecycleEvent.DESTROY);
    super.onDestroy();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    lifecycleStreams.notifyOnActivityResult(requestCode, resultCode, data);
  }
}
