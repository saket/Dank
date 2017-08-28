package me.saket.dank.widgets.binoculars;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * A ViewGroup that can be dismissed by flicking it in any direction.
 */
public class FlickDismissLayout extends FrameLayout {

  private FlickGestureListener flickGestureListener;

  public FlickDismissLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  /**
   * @return Return true to steal motion events from the children and have them dispatched to this ViewGroup
   * through onTouchEvent(). The current target will receive an ACTION_CANCEL event, and no further messages
   * will be delivered here.
   */
  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    boolean intercepted = flickGestureListener.onTouch(this, ev);
    return intercepted || super.onInterceptTouchEvent(ev);
  }

  @Override
  @SuppressLint("ClickableViewAccessibility")
  public boolean onTouchEvent(MotionEvent event) {
    boolean handled = flickGestureListener.onTouch(this, event);
    return handled || super.onTouchEvent(event);
  }

  @Override
  public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    super.requestDisallowInterceptTouchEvent(disallowIntercept);
  }

  public void setFlickGestureListener(FlickGestureListener flickGestureListener) {
    this.flickGestureListener = flickGestureListener;
  }
}
