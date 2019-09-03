package me.saket.dank.widgets.binoculars;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;

import androidx.annotation.FloatRange;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

/**
 * Listeners for a flick gesture and also moves around the View with user's finger.
 */
public class FlickGestureListener implements View.OnTouchListener {

  private static final Interpolator ANIM_INTERPOLATOR = new FastOutSlowInInterpolator();
  private static final boolean ROTATION_ENABLED = true;
  public static final float DEFAULT_FLICK_THRESHOLD = 0.3f;

  @FloatRange(from = 0, to = 1) private float flickThresholdSlop;

  private final int touchSlop;                // Min. distance to move before registering a gesture.
  private final int maximumFlingVelocity;     // Px per second.
  private GestureCallbacks gestureCallbacks;
  private float downX, downY;
  private float lastTouchX;
  private float lastTouchY;
  private int lastAction = -1;
  private boolean touchStartedOnLeftSide;
  private VelocityTracker velocityTracker;
  private boolean verticalScrollRegistered;
  private boolean gestureCanceledUntilNextTouchDown;
  private OnGestureIntercepter onGestureIntercepter;
  private ContentHeightProvider contentHeightProvider;
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

  public interface ContentHeightProvider {
    /**
     * Height of the media content multiplied by its zoomed in ratio. Only used for animating the content out
     * of the window when a flick is registered.
     */
    int getContentHeightForDismissAnimation();

    /**
     * Used for calculating if the content can be dismissed on finger up.
     */
    int getContentHeightForCalculatingThreshold();
  }

  public interface GestureCallbacks {
    /**
     * Called when the View has been flicked and the Activity should be dismissed.
     *
     * @param flickAnimationDuration Time the Activity should wait to finish for the flick animation to complete.
     */
    void onFlickDismissEnd(long flickAnimationDuration);

    /**
     * Called while this View is being moved around.
     *
     * @param moveRatio Distance moved (from the View's original position) as a ratio of the View's height.
     */
    void onMoveMedia(@FloatRange(from = -1, to = 1) float moveRatio);
  }

  public FlickGestureListener(ViewConfiguration viewConfiguration) {
    touchSlop = viewConfiguration.getScaledTouchSlop();
    maximumFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();

    // Default gesture intercepter: don't intercept anything.
    onGestureIntercepter = o -> false;
  }

  public void setOnGestureIntercepter(OnGestureIntercepter intercepter) {
    onGestureIntercepter = intercepter;
  }

  /**
   * Set minimum distance the user's finger should move (in percentage of the View's dimensions) after which a flick
   * can be registered.
   */
  public void setFlickThresholdSlop(@FloatRange(from = 0, to = 1) float flickThresholdSlop) {
    this.flickThresholdSlop = flickThresholdSlop;
  }

  public void setGestureCallbacks(GestureCallbacks gestureCallbacks) {
    this.gestureCallbacks = gestureCallbacks;
  }

  public void setContentHeightProvider(ContentHeightProvider contentHeightProvider) {
    this.contentHeightProvider = contentHeightProvider;
  }

  @Override
  @SuppressLint("ClickableViewAccessibility")
  public boolean onTouch(View view, MotionEvent event) {
    float touchX = event.getRawX();
    float touchY = event.getRawY();

    float distanceX = touchX - downX;
    float distanceY = touchY - downY;
    float distanceXAbs = Math.abs(distanceX);
    float distanceYAbs = Math.abs(distanceY);
    float deltaX = touchX - lastTouchX;
    float deltaY = touchY - lastTouchY;

    // Since both intercept() and touch() call this listener, we get duplicate ACTION_DOWNs.
    if (touchX == lastTouchX && touchY == lastTouchY && lastAction == event.getAction()) {
      // Clear VelocityTracker if this condition is removed.
      return false;
    }

    lastTouchX = touchX;
    lastTouchY = touchY;
    lastAction = event.getAction();

    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        downX = touchX;
        downY = touchY;
        touchStartedOnLeftSide = touchX < view.getWidth() / 2;
        if (velocityTracker == null) {
          velocityTracker = VelocityTracker.obtain();
        }
        //else {
        // This is required because ACTION_DOWN is received twice.
        //velocityTracker.clear();
        //}
        velocityTracker.addMovement(event);
        return false;

      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        if (verticalScrollRegistered) {
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
              // Distance moved wasn't enough to dismiss.
              animateViewBackToPosition(view);
            }
          }
        }

        velocityTracker.recycle();
        velocityTracker = null;
        verticalScrollRegistered = false;
        gestureInterceptedUntilNextTouchDown = false;
        gestureCanceledUntilNextTouchDown = false;
        return false;

      case MotionEvent.ACTION_MOVE:
        if (gestureInterceptedUntilNextTouchDown || gestureCanceledUntilNextTouchDown) {
          return false;
        }

        // The listener only gets once chance to block the flick -- only if it's not already being moved.
        if (!verticalScrollRegistered && onGestureIntercepter != null && onGestureIntercepter.shouldIntercept(deltaY)) {
          gestureInterceptedUntilNextTouchDown = true;
          return false;
        }

        boolean isScrollingVertically = distanceYAbs > touchSlop && distanceYAbs > distanceXAbs;
        boolean isScrollingHorizontally = distanceXAbs > touchSlop && distanceYAbs < distanceXAbs;

        // Avoid reading the gesture if the user is scrolling the horizontal list.
        if (!verticalScrollRegistered && isScrollingHorizontally) {
          gestureCanceledUntilNextTouchDown = true;
          return false;
        }

        if (verticalScrollRegistered || isScrollingVertically) {
          verticalScrollRegistered = true;

          view.setTranslationX(view.getTranslationX() + deltaX);
          view.setTranslationY(view.getTranslationY() + deltaY);

          view.getParent().requestDisallowInterceptTouchEvent(true);

          // Rotate the card because we naturally make a swipe gesture in a circular path while holding our phones.
          if (ROTATION_ENABLED) {
            float moveRatioDelta = deltaY / view.getHeight();
            view.setPivotY(0);
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
    animateViewFlick(view, downwards, 200);
  }

  @SuppressWarnings("ConstantConditions")
  private void animateViewFlick(View view, boolean downwards, long flickAnimDuration) {
    if (view.getPivotY() != 0f) {
      throw new AssertionError("Formula used for calculating distance rotated only works if the pivot is at (x,0");
    }

    float rotationAngle = view.getRotation();
    int distanceRotated = (int) Math.ceil(Math.abs(Math.sin(Math.toRadians(rotationAngle)) * view.getWidth() / 2));
    int throwDistance = distanceRotated + Math.max(contentHeightProvider.getContentHeightForDismissAnimation(), view.getRootView().getHeight());

    view.animate().cancel();
    view.animate()
        .translationY(downwards ? throwDistance : -throwDistance)
        .withStartAction(() -> gestureCallbacks.onFlickDismissEnd(flickAnimDuration))
        .setDuration(flickAnimDuration)
        .setInterpolator(ANIM_INTERPOLATOR)
        .setUpdateListener(animation -> dispatchOnPhotoMoveCallback(view))
        .start();
  }

  private boolean hasFingerMovedEnoughToFlick(float distanceYAbs) {
    //Timber.d("hasFingerMovedEnoughToFlick()");
    //Timber.i("Content h: %s", contentHeightProvider.getContentHeightForCalculatingThreshold());
    //Timber.i("flickThresholdSlop: %s", flickThresholdSlop);
    //Timber.i("distanceYAbs: %s", distanceYAbs);
    float thresholdDistanceY = contentHeightProvider.getContentHeightForCalculatingThreshold() * flickThresholdSlop;
    return distanceYAbs > thresholdDistanceY;
  }
}
