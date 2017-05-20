package me.saket.dank.utils;

import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Override what you want.
 */
public abstract class SimpleOnChildAttachStateChangeListener implements RecyclerView.OnChildAttachStateChangeListener {
  @Override
  public void onChildViewAttachedToWindow(View view) {

  }

  @Override
  public void onChildViewDetachedFromWindow(View view) {

  }
}
