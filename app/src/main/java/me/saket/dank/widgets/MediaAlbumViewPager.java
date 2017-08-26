package me.saket.dank.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;

import timber.log.Timber;

/**
 * Intercepts scrolling if the current image can be panned further.
 */
public class MediaAlbumViewPager extends ViewPager {

  public MediaAlbumViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  /**
   * Called by ViewPager for all children Views in a recursive way (top to bottom).
   */
  @Override
  protected boolean canScroll(View view, boolean checkView, int dx, int x, int y) {
    if (view instanceof ZoomableImageView) {
      return ((ZoomableImageView) view).canPanAnyFurtherHorizontally(dx);

    } else {
      if (view instanceof SeekBar) {
        Timber.i("canScroll() -> view: %s", view);
      }
      return super.canScroll(view, checkView, dx, x, y);
    }
  }

  @Override
  @SuppressLint("ClickableViewAccessibility")
  public boolean onTouchEvent(MotionEvent event) {
    // Only handle one finger touches! otherwise, the user probably does want to scale/pan
    return event.getPointerCount() == 1 && super.onTouchEvent(event);
  }
}
