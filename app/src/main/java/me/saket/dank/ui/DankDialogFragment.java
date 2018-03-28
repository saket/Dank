package me.saket.dank.ui;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;

import me.saket.dank.utils.lifecycle.LifecycleOwnerDialogFragment;

public class DankDialogFragment extends LifecycleOwnerDialogFragment {

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
}
