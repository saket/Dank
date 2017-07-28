package me.saket.dank.ui.media;

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
  private static final boolean ROTATION_ENABLED = false;

  @FloatRange(from = 0, to = 1) private float flickThresholdSlop;
  private ZoomableImageView imageView;
  private final int touchSlop;                // Min. distance to move before registering a gesture.
  private final int maximumFlingVelocity;     // Px per second.
  private GestureCallbacks gestureCallbacks;
  private float downX, downY;
  private float lastTouchY;
  private boolean touchStartedOnLeftSide;
  private VelocityTracker velocityTracker;
  private OnGestureIntercepter onGestureIntercepter;
  private boolean isListeningToGesturesSinceDown;

  public interface OnGestureIntercepter {
    /**
     * Called everytime a touch event is registered.
     *
     * @return True to intercept the gesture, false otherwise to let it go.
     */
    boolean shouldIntercept();
  }

  public interface GestureCallbacks {
    /**
     * Called when the View gets flicked and the Activity should be dismissed.
     */
    void onPhotoFlick();

    /**
     * Called while this View is being moved around.
     *
     * @param moveRatio Distance moved (from the View's original position) as a ratio of the View's height.
     */
    void onPhotoMove(@FloatRange(from = -1, to = 1) float moveRatio);
  }

  public FlickGestureListener(ViewConfiguration viewConfiguration) {
    touchSlop = viewConfiguration.getScaledTouchSlop();
    maximumFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
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

  public void setOnGestureIntercepter(OnGestureIntercepter onGestureIntercepter) {
    this.onGestureIntercepter = onGestureIntercepter;
  }

  @Override
  public boolean onTouch(View view, MotionEvent event) {
    float touchX = event.getRawX();
    float touchY = event.getRawY();

    float distanceY = touchY - downY;
    float distanceXAbs = Math.abs(touchX - downX);
    float distanceYAbs = Math.abs(distanceY);
    float deltaDistanceY = touchY - lastTouchY;

    lastTouchY = touchY;
    boolean isListeningToGestures = onGestureIntercepter.shouldIntercept();

    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        isListeningToGesturesSinceDown = isListeningToGestures;
        if (!isListeningToGestures) {
          return false;
        }

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
        return true;

      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        if (!isListeningToGestures) {
          return false;
        }

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
        velocityTracker.recycle();
        velocityTracker = null;
        return true;

      case MotionEvent.ACTION_MOVE:
        if (!isListeningToGesturesSinceDown || !onGestureIntercepter.shouldIntercept()) {
          return false;
        }

        if (distanceYAbs > touchSlop && distanceYAbs > distanceXAbs) {
          // Distance enough to register a gesture.
          view.setTranslationY(view.getTranslationY() + deltaDistanceY);

          // Rotate the card because we naturally make a swipe gesture in a circular path while holding our phones.
          if (ROTATION_ENABLED) {
            float moveRatioDelta = deltaDistanceY / view.getHeight();
            view.setRotation(view.getRotation() + moveRatioDelta * 20 * (touchStartedOnLeftSide ? -1 : 1));
          }

          // Send callback so that the background dim can be faded in/out.
          dispatchOnPhotoMoveCallback(view);

          // Track the velocity so that we can later figure out if this View was fling'd (instead of dragged).
          velocityTracker.addMovement(event);
          return true;
        }
        return false;
    }
    return false;
  }

  private void dispatchOnPhotoMoveCallback(View view) {
    float moveRatio = view.getTranslationY() / view.getHeight();
    gestureCallbacks.onPhotoMove(moveRatio);
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
        .withEndAction(() -> gestureCallbacks.onPhotoFlick())
        .setDuration(flickAnimDuration)
        .setInterpolator(ANIM_INTERPOLATOR)
        .start();
  }

  private boolean hasFingerMovedEnoughToFlick(float distanceYAbs) {
    if (imageView.getDrawable() == null) {
      return false;
    }
    float thresholdDistanceY = imageView.getDrawable().getIntrinsicHeight() * flickThresholdSlop;
    return distanceYAbs > thresholdDistanceY;
  }
}
