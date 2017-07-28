package me.saket.dank.widgets.binoculars;

import android.support.annotation.FloatRange;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;

import me.saket.dank.widgets.ZoomableImageView;

/**
 * Listeners for a flick gesture and also moves around the View with user's finger.
 */
public class FlickGestureListener implements View.OnTouchListener {

  private static final Interpolator ANIM_INTERPOLATOR = new FastOutSlowInInterpolator();
  private static final boolean ROTATION_ENABLED = true;

  @FloatRange(from = 0, to = 1) private float flickThresholdSlop;

  private ZoomableImageView imageView;
  private final int touchSlop;                // Min. distance to move before registering a gesture.
  private final int maximumFlingVelocity;     // Px per second.
  private GestureCallbacks gestureCallbacks;
  private float downX, downY;
  private float lastTouchX;
  private float lastTouchY;
  private boolean touchStartedOnLeftSide;
  private VelocityTracker velocityTracker;
  private boolean isSwiping;
  private OnGestureIntercepter onGestureIntercepter;
  private boolean gestureInterceptedUntilNextTouchDown;

  public interface OnGestureIntercepter {
    /**
     * Called once everytime a scroll gesture is registered. When this returns true, gesture detection is
     * skipped until the next touch-down is registered.
     *
     * @return True to intercept the gesture, false otherwise to let it go.
     */
    boolean shouldIntercept(float deltaY);
  }

  public interface GestureCallbacks {
    /**
     * Called when the View gets flicked and the Activity should be dismissed.
     */
    void onFlickDismiss();

    /**
     * Called while this View is being moved around.
     *
     * @param moveRatio Distance moved (from the View's original position) as a ratio of the View's height.
     */
    void onMoveMedia(@FloatRange(from = -1, to = 1) float moveRatio);

    /**
     * Stupid GestureViews has fucked up the way touch events are passed to the pager so we've to manually
     * disable scrolling.
     */
    void setMediaListScrollingBlocked(boolean blocked);
  }

  public FlickGestureListener(ViewConfiguration viewConfiguration) {
    touchSlop = viewConfiguration.getScaledTouchSlop();
    maximumFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
  }

  public void setOnGestureIntercepter(OnGestureIntercepter intercepter) {
    onGestureIntercepter = intercepter;
  }

  /**
   * Set maximum distance the user's finger should move (in percentage of the View's dimensions) after which a flick should be registered.
   *
   * @param imageView The imageView whose Drawable's height will be matched against the flick distance..
   */
  public void setFlickThresholdSlop(@FloatRange(from = 0, to = 1) float flickThresholdSlop, ZoomableImageView imageView) {
    this.flickThresholdSlop = flickThresholdSlop;
    this.imageView = imageView;
  }

  public void setGestureCallbacks(GestureCallbacks gestureCallbacks) {
    this.gestureCallbacks = gestureCallbacks;
  }

  @Override
  public boolean onTouch(View view, MotionEvent event) {
    float touchX = event.getRawX();
    float touchY = event.getRawY();

    float distanceX = touchX - downX;
    float distanceY = touchY - downY;
    float distanceXAbs = Math.abs(distanceX);
    float distanceYAbs = Math.abs(distanceY);
    float deltaX = touchX - lastTouchX;
    float deltaY = touchY - lastTouchY;

    lastTouchX = touchX;
    lastTouchY = touchY;

    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        downX = touchX;
        downY = touchY;
        touchStartedOnLeftSide = touchX < view.getWidth() / 2;
        if (velocityTracker == null) {
          velocityTracker = VelocityTracker.obtain();
        } else {
          // This is required because ACTION_DOWN is received twice.
          velocityTracker.clear();
        }
        velocityTracker.addMovement(event);
        return false;

      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        if (isSwiping) {
          boolean flickRegistered = hasFingerMovedEnoughToFlick(distanceYAbs);
          boolean wasSwipedDownwards = distanceY > 0;

          if (flickRegistered) {
            animateViewFlick(view, wasSwipedDownwards);

          } else {
            // Figure out if the View was fling'd and if the velocity + swiped distance is enough to dismiss this View.
            velocityTracker.computeCurrentVelocity(1_000 /* px per second */);
            float yVelocityAbs = Math.abs(velocityTracker.getYVelocity());
            int requiredYVelocity = view.getHeight() * 6 / 10;
            int minSwipeDistanceForFling = view.getHeight() / 10;

            if (yVelocityAbs > requiredYVelocity && yVelocityAbs < maximumFlingVelocity && distanceYAbs >= minSwipeDistanceForFling) {
              // Fling detected!
              animateViewFlick(view, wasSwipedDownwards, 100);

            } else {
              // Distance moved wasn't enough to dismiss. Move back to original position.
              animateViewBackToPosition(view);
            }
          }

          gestureCallbacks.setMediaListScrollingBlocked(false);
        }

        velocityTracker.recycle();
        velocityTracker = null;
        isSwiping = false;
        gestureInterceptedUntilNextTouchDown = false;
        return false;

      case MotionEvent.ACTION_MOVE:
        if (gestureInterceptedUntilNextTouchDown) {
          return false;
        }

        // The listener only gets once chance to block the flick -- only if it's not already being moved.
        if (!isSwiping && onGestureIntercepter.shouldIntercept(deltaY)) {
          gestureInterceptedUntilNextTouchDown = true;
          return false;
        }

        boolean isScrollingVertically = distanceYAbs > touchSlop && distanceYAbs > distanceXAbs;

        if (isSwiping || isScrollingVertically) {
          isSwiping = true;

          view.setTranslationX(view.getTranslationX() + deltaX);
          view.setTranslationY(view.getTranslationY() + deltaY);

          gestureCallbacks.setMediaListScrollingBlocked(true);

          // Rotate the card because we naturally make a swipe gesture in a circular path while holding our phones.
          if (ROTATION_ENABLED) {
            float moveRatioDelta = deltaY / view.getHeight();
            view.setRotation(view.getRotation() + moveRatioDelta * 20 * (touchStartedOnLeftSide ? -1 : 1));
          }

          // Send callback so that the background dim can be faded in/out.
          dispatchOnPhotoMoveCallback(view);

          // Track the velocity so that we can later figure out if this View was fling'd (instead of dragged).
          velocityTracker.addMovement(event);
          return true;

        } else {
          return false;
        }

      default:
        return false;
    }
  }

  private void dispatchOnPhotoMoveCallback(View view) {
    float moveRatio = view.getTranslationY() / view.getHeight();
    gestureCallbacks.onMoveMedia(moveRatio);
  }

  private void animateViewBackToPosition(View view) {
    view.animate().cancel();
    view.animate()
        .translationX(0f)
        .translationY(0f)
        .rotation(0f)
        .setDuration(200)
        .setUpdateListener(animation -> dispatchOnPhotoMoveCallback(view))
        .setInterpolator(ANIM_INTERPOLATOR)
        .start();
  }

  private void animateViewFlick(View view, boolean downwards) {
    animateViewFlick(view, downwards, 100);
  }

  @SuppressWarnings("ConstantConditions")
  private void animateViewFlick(View view, boolean downwards, long flickAnimDuration) {
    int parentHeight = ((ViewGroup) view.getParent()).getHeight();

    view.animate().cancel();
    view.animate()
        .translationY(downwards ? parentHeight : -parentHeight)
        .withEndAction(() -> gestureCallbacks.onFlickDismiss())
        .setDuration(flickAnimDuration)
        .setInterpolator(ANIM_INTERPOLATOR)
        .start();
  }

  private boolean hasFingerMovedEnoughToFlick(float distanceYAbs) {
    if (imageView.getDrawable() == null) {
      return false;
    }
    float thresholdDistanceY = imageView.getVisibleZoomedImageHeight() * flickThresholdSlop;
    return distanceYAbs > thresholdDistanceY;
  }
}
