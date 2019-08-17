package me.saket.dank.utils;

import android.content.Context;
import androidx.recyclerview.widget.LinearSmoothScroller;

public class LinearSmoothScrollerWithVerticalSnapPref extends LinearSmoothScroller {
  private final int snapPref;

  public LinearSmoothScrollerWithVerticalSnapPref(Context context, int snapPref) {
    super(context);
    this.snapPref = snapPref;
  }

  @Override
  protected int getVerticalSnapPreference() {
    return snapPref;
  }
}
