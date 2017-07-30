package me.saket.dank.widgets;

import android.content.Context;
import android.graphics.RectF;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.alexvasilkov.gestures.views.GestureImageView;

/**
 * Intercepts scrolling if the current image can be panned further.
 */
public class MediaAlbumViewPager extends ViewPager {

  public MediaAlbumViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected boolean canScroll(View view, boolean checkView, int dx, int x, int y) {
    if (view instanceof GestureImageView) {
      GestureImageView gestureImageView = (GestureImageView) view;
      float minZoom = gestureImageView.getController().getStateController().getEffectiveMinZoom();
      float zoom = gestureImageView.getController().getState().getZoom();

      // if zoom factor == min zoom factor => just let the view pager handle the scroll
      float eps = 0.001f;
      if (Math.abs(minZoom - zoom) < eps) {
        return false;
      }

      RectF effectiveMovementArea = new RectF();
      gestureImageView.getController().getStateController().getMovementArea(gestureImageView.getController().getState(), effectiveMovementArea);

      float stateX = gestureImageView.getController().getState().getX();
      float width = effectiveMovementArea.width();

      // If user reached left edge && is swiping left => just let the view pager handle the scroll
      if (Math.abs(stateX) < eps && dx > 0) {
        return false;
      }

      // If user reached right edge && is swiping right => just let the view pager handle the scroll
      return !(Math.abs(stateX + width) < eps && dx < 0);

    } else {
      return super.canScroll(view, checkView, dx, x, y);
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    // Only handle one finger touches! otherwise, the user probably does want to scale/pan
    return event.getPointerCount() == 1 && super.onTouchEvent(event);
  }
}
