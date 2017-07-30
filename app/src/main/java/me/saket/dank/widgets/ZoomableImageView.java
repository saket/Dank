package me.saket.dank.widgets;

import android.content.Context;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.alexvasilkov.gestures.GestureController;
import com.alexvasilkov.gestures.Settings;
import com.alexvasilkov.gestures.State;
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

  public float getZoomedImageHeight() {
    return (float) getController().getSettings().getImageH() * getZoom();
  }

  public float getZoom() {
    return getController().getState().getZoom();
  }

  /**
   * Whether the image can be panned anymore vertically, upwards or downwards depending upon <var>downwardPan</var>.
   */
  public boolean canPanFurtherVertically(boolean downwardPan) {
    State state = getController().getState();
    getController().getStateController().getMovementArea(state, IMAGE_MOVEMENT_RECT);

    return (!downwardPan && State.compare(state.getY(), IMAGE_MOVEMENT_RECT.bottom) < 0f)
        || (downwardPan && State.compare(state.getY(), IMAGE_MOVEMENT_RECT.top) > 0f);
  }

  private final RectF IMAGE_MOVEMENT_RECT = new RectF();

  /**
   * Whether the image can be panned anymore horizontally.
   */
  public boolean canPanFurtherHorizontally(boolean towardsRight) {
    State state = getController().getState();
    getController().getStateController().getMovementArea(state, IMAGE_MOVEMENT_RECT);

    return (towardsRight && State.compare(state.getX(), IMAGE_MOVEMENT_RECT.right) < 0f)
        || (!towardsRight && State.compare(state.getX(), IMAGE_MOVEMENT_RECT.left) > 0f);
  }

  public void setGestureRotationEnabled(boolean rotationEnabled) {
    Settings settings = getController().getSettings();
    settings.setRotationEnabled(rotationEnabled);
    settings.setRestrictRotation(rotationEnabled);
  }

  public void enableScrollInViewPager(ViewPager pager) {
    getController().enableScrollInViewPager(pager);
  }

  /**
   * Reset zoom, rotation, etc.
   */
  public void resetState() {
    getController().resetState();
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
