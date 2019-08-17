package me.saket.dank.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * A ViewGroup that forwards all its touch events to its 0th child View. Used as a replacement for touch-delegate.
 */
public class TouchForwardingFrameLayout extends FrameLayout {

  private View childView;

  public TouchForwardingFrameLayout(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    if (getPaddingStart() > 0 || getPaddingEnd() > 0) {
      throw new AssertionError("Horizontal paddings aren't supported. " +
          "The coordinates of touch events will have to be transformed in order to do that.");
    }
  }

  @Override
  public void addView(View child, ViewGroup.LayoutParams params) {
    childView = child;
    super.addView(child, params);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    return childView.dispatchTouchEvent(ev);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    throw new UnsupportedOperationException();
  }

  @Override
  @SuppressLint("ClickableViewAccessibility")
  public boolean onTouchEvent(MotionEvent event) {
    throw new UnsupportedOperationException();
  }
}
