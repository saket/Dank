package me.saket.dank.widgets.binoculars;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import me.saket.dank.ui.media.FlickGestureListener;

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
    return flickGestureListener.onTouch(this, ev) && super.onInterceptTouchEvent(ev);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return flickGestureListener.onTouch(this, event);
  }

  public void setFlickGestureListener(FlickGestureListener flickGestureListener) {
    this.flickGestureListener = flickGestureListener;
  }
}
