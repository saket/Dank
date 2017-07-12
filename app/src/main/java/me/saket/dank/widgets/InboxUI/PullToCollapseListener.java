package me.saket.dank.widgets.InboxUI;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles pull-to-dispatch gesture for an {@link ExpandablePageLayout}.
 */
public class PullToCollapseListener implements View.OnTouchListener {

  private static final float COLLAPSE_THRESHOLD_DISTANCE_FACTOR = 0.85f;   // This gets multiplied with the toolbar height
  private final int touchSlop;

  private ExpandablePageLayout expandablePage;
  private List<OnPullListener> onPullListeners = new ArrayList<>(3);
  private int collapseDistanceThreshold;
  private float downX;
  private float downY;
  private float lastMoveY;
  private boolean eligibleForCollapse;
  private Boolean horizontalSwipingConfirmed;
  private Boolean interceptedUntilNextGesture;

  public interface OnPullListener {
    /**
     * Called when the user is pulling down / up the expandable page or the list.
     *
     * @param deltaY              Delta translation-Y since the last onPull call.
     * @param currentTranslationY Current translation-Y of the page.
     * @param upwardPull          Whether or not the page is being pulled in the upward direction.
     * @param deltaUpwardPull     Whether or not the last delta-pull was made in the upward direction.
     * @param collapseEligible    Whether or not the pull distance was enough to trigger a collapse.
     */
    void onPull(float deltaY, float currentTranslationY, boolean upwardPull, boolean deltaUpwardPull, boolean collapseEligible);

    /**
     * Called when the user's finger is lifted.
     *
     * @param collapseEligible Whether or not the pull distance was enough to trigger a collapse.
     */
    void onRelease(boolean collapseEligible);
  }

  public PullToCollapseListener(Context context, ExpandablePageLayout expandablePage) {
    touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    this.expandablePage = expandablePage;
  }

  public void addOnPullListeners(OnPullListener listener) {
    onPullListeners.add(listener);
  }

  /**
   * The distance after which the page can collapse when pulled.
   */
  public void setCollapseDistanceThreshold(int threshold) {
    collapseDistanceThreshold = threshold;
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        downX = event.getRawX();
        downY = event.getRawY();
        lastMoveY = downY;

        eligibleForCollapse = false;
        interceptedUntilNextGesture = horizontalSwipingConfirmed = null;
        return false;

      case MotionEvent.ACTION_MOVE: {
        // Keep ignoring horizontal swipes.
        if (horizontalSwipingConfirmed != null && horizontalSwipingConfirmed) {
          return false;
        }
        if (interceptedUntilNextGesture != null && interceptedUntilNextGesture) {
          return false;
        }

        float deltaY = event.getRawY() - lastMoveY;
        float totalSwipeDistanceX = event.getRawX() - downX;
        float totalSwipeDistanceY = event.getRawY() - downY;
        float totalSwipeDistanceXAbs = Math.abs(totalSwipeDistanceX);
        float totalSwipeDistanceYAbs = Math.abs(totalSwipeDistanceY);

        if (totalSwipeDistanceYAbs < touchSlop && totalSwipeDistanceXAbs < touchSlop) {
          return false;
        }

        // When it's confirmed that the movement distance is > touchSlop and this is indeed a gesture,
        // we must check for two things:
        // 1. Whether or not this is a horizontal swipe.
        // 2. Whether a registered intercepter wants to intercept this gesture.
        // These two checks should only happen once per gesture, just when the gesture starts. The
        // flags will reset when the finger is lifted.

        // Ignore horizontal swipes (Step 1)
        if (horizontalSwipingConfirmed == null) {
          horizontalSwipingConfirmed = totalSwipeDistanceXAbs > totalSwipeDistanceYAbs;

          // Lazy hack: We must also cancel any ongoing release animation. But it should only be performed
          // once, at the starting of a gesture. Let's just do it here.
          if (expandablePage.isExpanded()) {
            cancelAnyOngoingAnimations();
          }

          if (horizontalSwipingConfirmed) {
            return false;
          }
        }

        boolean deltaUpwardSwipe = deltaY < 0;

        // Avoid registering this gesture if the page doesn't want us to. Mostly used when the page also
        // has a scrollable child.
        if (interceptedUntilNextGesture == null) {
          interceptedUntilNextGesture = expandablePage.handleOnPullToCollapseIntercept(event, downX, downY, deltaUpwardSwipe);
          if (interceptedUntilNextGesture) {
            return false;
          } else {
            expandablePage.getParent().requestDisallowInterceptTouchEvent(true);
          }
        }

        boolean upwardSwipe = totalSwipeDistanceY < 0f;
        float resistanceFactor = 4f;

        // If the gesture has covered a distance >= the toolbar height, mark this gesture eligible
        // for collapsible when the finger is lifted
        int collapseThresholdDistance = (int) (collapseDistanceThreshold * COLLAPSE_THRESHOLD_DISTANCE_FACTOR);
        eligibleForCollapse = upwardSwipe
            ? expandablePage.getTranslationY() <= -collapseThresholdDistance
            : expandablePage.getTranslationY() >= collapseThresholdDistance;
        float resistedDeltaY = deltaY / resistanceFactor;

        // Once it's eligible, start resisting more as an indicator that the
        // page / list is being overscrolled. This will also prevent the user
        // to swipe all the way down or up
        if (eligibleForCollapse && expandablePage.getTranslationY() != 0f) {
          float extraResistance = collapseDistanceThreshold / Math.abs(expandablePage.getTranslationY());
          resistedDeltaY *= extraResistance / 2;
        }

        // Move page
        float translationY = expandablePage.getTranslationY() + resistedDeltaY;
        expandablePage.setTranslationY(translationY);
        dispatchPulledCallback(resistedDeltaY, translationY, upwardSwipe, deltaUpwardSwipe);

        lastMoveY = event.getRawY();
        return true;
      }

      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        // Collapse or restore the page when the finger is lifted, depending on
        // the pull distance
        float totalSwipeDistanceY = event.getRawY() - downY;
        if (Math.abs(totalSwipeDistanceY) >= touchSlop) {
          dispatchReleaseCallback();
        }
        break;
    }

    return false;
  }

  private void dispatchReleaseCallback() {
    for (int i = 0; i < onPullListeners.size(); i++) {
      OnPullListener onPullListener = onPullListeners.get(i);
      onPullListener.onRelease(eligibleForCollapse);
    }
  }

  private void dispatchPulledCallback(float deltaY, float translationY, boolean upwardPull, boolean deltaUpwardPull) {
    for (int i = 0; i < onPullListeners.size(); i++) {
      OnPullListener onPullListener = onPullListeners.get(i);
      onPullListener.onPull(deltaY, translationY, upwardPull, deltaUpwardPull, eligibleForCollapse);
    }
  }

  private void cancelAnyOngoingAnimations() {
    expandablePage.stopAnyOngoingPageAnimation();
  }
}
