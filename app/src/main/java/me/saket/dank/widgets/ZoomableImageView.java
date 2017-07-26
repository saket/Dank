package me.saket.dank.widgets;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.alexvasilkov.gestures.GestureController;
import com.alexvasilkov.gestures.Settings;
import com.alexvasilkov.gestures.views.GestureImageView;

/**
 * This wrapper exists so that we can easily change libraries in the future. It has happened once so far
 * and can happen again.
 */
public class ZoomableImageView extends GestureImageView {

  private static final float MAX_OVER_ZOOM = 4f;
  private static final float MIN_OVER_ZOOM = 1f;

  private GestureDetector gestureDetector;

  public ZoomableImageView(Context context, AttributeSet attrs) {
    super(context, attrs);

    getController().getSettings().setOverzoomFactor(MAX_OVER_ZOOM);
    getController().getSettings().setFillViewport(true);
    getController().getSettings().setFitMethod(Settings.Fit.HORIZONTAL);

    getController().setOnGesturesListener(new GestureController.SimpleOnGestureListener() {
      @Override
      public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
        performClick();
        return true;
      }

      @Override
      public void onUpOrCancel(@NonNull MotionEvent event) {
        // Bug workaround: Image zoom stops working after first overzoom. Resetting it when the
        // finger is lifted seems to solve the problem.
        getController().getSettings().setOverzoomFactor(MAX_OVER_ZOOM);
      }
    });

    // Bug workarounds: GestureImageView doesn't request parent ViewGroups to stop intercepting touch
    // events when it starts consuming them to zoom.
    gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
      @Override
      public boolean onDoubleTapEvent(MotionEvent e) {
        getParent().requestDisallowInterceptTouchEvent(true);
        return super.onDoubleTapEvent(e);
      }
    });
  }

  public void setGravity(int gravity) {
    getController().getSettings().setGravity(gravity);
  }

  /**
   * Calculate height of the image that is currently visible.
   */
  public float getVisibleZoomedImageHeight() {
    float zoomedImageHeight = getZoomedImageHeight();

    // Subtract the portion that has gone outside limits due to zooming in, because they are longer visible.
    float heightNotVisible = getController().getState().getY();
    if (heightNotVisible < 0) {
      zoomedImageHeight += heightNotVisible;
    }

    if (zoomedImageHeight > getHeight()) {
      zoomedImageHeight = getHeight();
    }

    return zoomedImageHeight;
  }

  private float getZoomedImageWidth() {
    return (float) getController().getSettings().getImageW() * getZoom();
  }

  private float getZoomedImageHeight() {
    return (float) getController().getSettings().getImageH() * getZoom();
  }

  public float getZoom() {
    return getController().getState().getZoom();
  }

  /**
   * Whether the image can be panned anymore vertically, upwards or downwards depending upon <var>upwardPan</var>.
   */
  public boolean canPanVertically(boolean upwardPan) {
    float imageY = getController().getState().getY();
    if (upwardPan) {
      return imageY != 0;
    } else {
      // Casting to int is intentional to ignore values like 1080.0002f.
      return getHeight() - (int) getZoomedImageHeight() != imageY;
    }
  }

  /**
   * Whether the image can be panned anymore horizontally.
   */
  public boolean canPanHorizontally(boolean towardsRight) {
    float imageX = getController().getState().getX();
    if (towardsRight) {
      return imageX < 0;
    } else {
      // Casting to int is intentional to ignore values like 1080.0002f.
      return getWidth() - (int) getZoomedImageWidth() != imageX;
    }
  }

  @Override
  public boolean onTouchEvent(@NonNull MotionEvent event) {
    gestureDetector.onTouchEvent(event);

    if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() == 2) {
      // Two-finger zoom is probably going to start. Disallow parent from intercepting this gesture.
      getParent().requestDisallowInterceptTouchEvent(true);
    }

    return super.onTouchEvent(event);
  }
}
