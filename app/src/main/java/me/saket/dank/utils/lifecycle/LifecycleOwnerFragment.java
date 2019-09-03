package me.saket.dank.utils.lifecycle;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;

public class LifecycleOwnerFragment extends Fragment implements LifecycleOwner<FragmentLifecycleEvent> {

  private FragmentLifecycleStreams lifecycleStreams;

  @Override
  public FragmentLifecycleStreams lifecycle() {
    return lifecycleStreams;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (lifecycleStreams == null) {
      lifecycleStreams = new FragmentLifecycleStreams();
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    lifecycleStreams.accept(FragmentLifecycleEvent.START);
  }

  @Override
  public void onResume() {
    super.onResume();
    lifecycleStreams.accept(FragmentLifecycleEvent.RESUME);
  }

  @Override
  public void onPause() {
    lifecycleStreams.accept(FragmentLifecycleEvent.PAUSE);
    super.onPause();
  }

  @Override
  public void onStop() {
    lifecycleStreams.accept(FragmentLifecycleEvent.STOP);
    super.onStop();
  }

  @Override
  public void onDestroy() {
    lifecycleStreams.accept(FragmentLifecycleEvent.DESTROY);
    super.onDestroy();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    lifecycleStreams.notifyOnActivityResult(requestCode, resultCode, data);
  }
}
