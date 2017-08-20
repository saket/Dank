package me.saket.dank.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * A ViewGroup that forwards all its touch events to its child View. Used as a replacement for touch-delegate.
 */
public class TouchForwardingFrameLayout extends FrameLayout {

  private View childView;
  private boolean isChildAViewGroup;

  public TouchForwardingFrameLayout(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void addView(View child, ViewGroup.LayoutParams params) {
    if (getChildCount() > 0) {
      throw new AssertionError("Can only have 1 child");
    }
    childView = child;
    isChildAViewGroup = child instanceof ViewGroup;
    super.addView(child, params);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    return childView.dispatchTouchEvent(ev);
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (isChildAViewGroup) {
      return ((ViewGroup) childView).onInterceptTouchEvent(ev);
    } else {
      return super.onInterceptTouchEvent(ev);
    }
  }

  @Override
  @SuppressLint("ClickableViewAccessibility")
  public boolean onTouchEvent(MotionEvent event) {
    return childView.onTouchEvent(event);
  }
}
