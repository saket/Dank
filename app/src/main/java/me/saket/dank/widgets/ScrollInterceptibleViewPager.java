package me.saket.dank.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.viewpager.widget.ViewPager;

/**
 * Intercepts scrolling if the current image can be panned further.
 */
public class ScrollInterceptibleViewPager extends ViewPager {

  private OnInterceptScrollListener onInterceptScrollListener;

  public interface OnInterceptScrollListener {
    /**
     * Return true if the scroll should be intercepted before it reaches this ViewPager.
     */
    boolean shouldInterceptScroll(View view, int deltaX, int touchX, int touchY);
  }

  public ScrollInterceptibleViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setOnInterceptScrollListener(OnInterceptScrollListener listener) {
    onInterceptScrollListener = listener;
  }

  /**
   * Called by ViewPager for all children Views in a recursive way (top to bottom).
   */
  @Override
  protected boolean canScroll(View view, boolean checkView, int dx, int x, int y) {
    boolean intercepted = onInterceptScrollListener.shouldInterceptScroll(view, dx, x, y);
    return intercepted || super.canScroll(view, checkView, dx, x, y);
  }

  @Override
  @SuppressLint("ClickableViewAccessibility")
  public boolean onTouchEvent(MotionEvent event) {
    try {
      // Only handle one finger touches! otherwise, the user is trying to scale/pan
      return event.getPointerCount() == 1 && super.onTouchEvent(event);

    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("pointerIndex out of range")) {
        // https://github.com/chrisbanes/PhotoView/issues/31#issuecomment-19803926
        return false;
      }
      throw e;
    }
  }

  // Touch events on seek-bar get delayed if it's inside a scrollable container.
  // This blocks that behavior.
  @Override
  public boolean shouldDelayChildPressedState() {
    return false;
  }
}
