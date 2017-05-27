package me.saket.dank.widgets.swipe;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class SwipeableLayout extends FrameLayout {

  private View swipeableChild;
  private BackgroundDrawable backgroundDrawable;
  private SwipeActions swipeActions;
  private SwipeActionIconView actionIconView;
  private SwipeAction activeSwipeAction;
  private boolean swipeDistanceThresholdCrossed;
  private ObjectAnimator translationAnimator;
  private FadingCircleDrawable swipeActionTriggerDrawable;

  private OnPerformSwipeActionListener onPerformSwipeActionListener;
  private SwipeActionIconProvider swipeActionIconProvider;

  public interface SwipeActionIconProvider {
    /**
     * Called when the visible swipe action changes.
     *
     * @param oldAction The action that was visible before. Null when this is the first action. This
     *                  can be used to figure out any icon animation for <var>newAction</var>'s icon.
     */
    void showSwipeActionIcon(SwipeActionIconView imageView, @Nullable SwipeAction oldAction, SwipeAction newAction);
  }

  public interface OnPerformSwipeActionListener {
    /**
     * Called when the finger is lifted on an action. Only called when the swipe threshold (presently at
     * 40% of the icon) is crossed.
     */
    void onPerformSwipeAction(SwipeAction action);
  }

  public SwipeableLayout(Context context, AttributeSet attrs) {
    super(context, attrs);

    backgroundDrawable = new BackgroundDrawable(new ColorDrawable(Color.TRANSPARENT), new ColorDrawable(Color.DKGRAY));
    setBackground(backgroundDrawable);

    setSwipeDistanceThresholdCrossed(false);  // Controls the background color's gray tint.

    setWillNotDraw(false);
    swipeActionTriggerDrawable = new FadingCircleDrawable();
    swipeActionTriggerDrawable.setCallback(this);
  }

  public void setSwipeActions(SwipeActions actions) {
    swipeActions = actions;
  }

  public void setSwipeActionIconProvider(SwipeActionIconProvider iconProvider) {
    swipeActionIconProvider = iconProvider;
  }

  public void setOnPerformSwipeActionListener(OnPerformSwipeActionListener listener) {
    onPerformSwipeActionListener = listener;
  }

  @Override
  public void addView(View child, int index, ViewGroup.LayoutParams params) {
    super.addView(child, index, params);

    if (child instanceof SwipeActionIconView) {
      actionIconView = ((SwipeActionIconView) child);
    } else {
      swipeableChild = child;
    }

    if (getChildCount() > 2) {
      throw new UnsupportedOperationException("SwipeableLayout only supports 2 child Views.");
    }
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    if (actionIconView == null) {
      throw new IllegalStateException("Action icon is missing");
    }
  }

// ======== SWIPE ======== //

  public void setSwipeTranslation(@FloatRange(from = -1f, to = 1f) float translationX) {
    if (!isLaidOut()) {
      throw new IllegalStateException("SwipeableLayout hasn't been measured yet!");
    }

    swipeableChild.setTranslationX(translationX);
    swipeActionTriggerDrawable.setBounds((int) translationX, 0, (int) (getWidth() + translationX), getHeight());

    if (translationX == 0f) {
      backgroundDrawable.animateColorTransition(Color.TRANSPARENT);
      setSwipeDistanceThresholdCrossed(false);
      activeSwipeAction = null;

    } else {
      boolean swipingFromEndToStart = translationX < 0f;
      SwipeAction swipeAction = swipingFromEndToStart
          ? swipeActions.endActions().findActionAtSwipeDistance(getWidth(), Math.abs(translationX), SwipeDirection.END_TO_START)
          : swipeActions.startActions().findActionAtSwipeDistance(getWidth(), Math.abs(translationX), SwipeDirection.START_TO_END);

      // Move the icon along with the View being swiped.
      if (swipingFromEndToStart) {
        actionIconView.setTranslationX(getWidth() - translationX);
      } else {
        actionIconView.setTranslationX(translationX - actionIconView.getWidth());
      }

      if (!isSettlingBackToPosition()) {
        if (activeSwipeAction != swipeAction) {
          SwipeAction oldAction = activeSwipeAction;
          activeSwipeAction = swipeAction;

          // Request an update to the icon.
          swipeActionIconProvider.showSwipeActionIcon(actionIconView, oldAction, swipeAction);

          // Animate the background color transition only if the swipe threshold is passed.
          backgroundDrawable.animateColorTransition(swipeAction.backgroundColor());
        }

        // A gray tint on the background is shown until the swipe threshold is crossed.
        boolean swipeThresholdCrossed = Math.abs(translationX) > actionIconView.getWidth() * 4 / 10;
        setSwipeDistanceThresholdCrossed(swipeThresholdCrossed);
      }
    }
  }

  /**
   * Animate the swipeable child to its original translation.
   */
  public void animateBackToPosition() {
    translationAnimator = ObjectAnimator.ofFloat(this, "swipeTranslation", getSwipeTranslation(), 0f);
    translationAnimator.setDuration(300);
    translationAnimator.setInterpolator(new FastOutSlowInInterpolator());
    translationAnimator.start();
  }

  /**
   * Whether the swipeable child is being animated back to original translation.
   */
  public boolean isSettlingBackToPosition() {
    return translationAnimator != null && translationAnimator.isRunning();
  }

  public void handleOnRelease() {
    if (hasCrossedSwipeDistanceThreshold()) {
      onPerformSwipeActionListener.onPerformSwipeAction(activeSwipeAction);

      boolean triggerFromStart = swipeActions.startActions().contains(activeSwipeAction);
      swipeActionTriggerDrawable.play(activeSwipeAction.backgroundColor(), triggerFromStart);
    }
  }

  public float getSwipeTranslation() {
    return swipeableChild.getTranslationX();
  }

  private void setSwipeDistanceThresholdCrossed(boolean thresholdCrossed) {
    if (hasCrossedSwipeDistanceThreshold() == thresholdCrossed) {
      return;
    }
    swipeDistanceThresholdCrossed = thresholdCrossed;
    backgroundDrawable.animateSwipeThresholdCrossedTransition(thresholdCrossed ? 0 : 255);
  }

  public boolean hasCrossedSwipeDistanceThreshold() {
    return swipeDistanceThresholdCrossed;
  }

  private static class BackgroundDrawable extends LayerDrawable {
    /**
     * Creates a new layer drawable with the list of specified layers.
     */
    public BackgroundDrawable(ColorDrawable backgroundColorDrawable, ColorDrawable swipeThresholdIndicatorDrawable) {
      super(new Drawable[] { backgroundColorDrawable, swipeThresholdIndicatorDrawable });
    }

    public ColorDrawable colorDrawable() {
      return (ColorDrawable) getDrawable(0);
    }

    /**
     * Used for indicating whether that the swipe threshold has been crossed.
     */
    public ColorDrawable swipeThresholdTintDrawable() {
      return (ColorDrawable) getDrawable(1);
    }

    /**
     * Animate the background layer's color.
     */
    public void animateColorTransition(@ColorInt int toColor) {
      ValueAnimator transitionAnimator = ValueAnimator.ofArgb(colorDrawable().getColor(), toColor);
      transitionAnimator.addUpdateListener(animation -> {
        colorDrawable().setColor(((Integer) animation.getAnimatedValue()));
      });
      transitionAnimator.setDuration(200);
      transitionAnimator.setInterpolator(new FastOutSlowInInterpolator());
      transitionAnimator.start();
    }

    /**
     * Animate the gray layer's alpha.
     */
    public void animateSwipeThresholdCrossedTransition(@IntRange(from = 0, to = 255) int toAlpha) {
      ObjectAnimator transitionAnimator = ObjectAnimator.ofInt(
          swipeThresholdTintDrawable(),
          "alpha",
          swipeThresholdTintDrawable().getAlpha(),
          toAlpha
      );
      transitionAnimator.setDuration(200);
      transitionAnimator.setInterpolator(new FastOutSlowInInterpolator());
      transitionAnimator.start();
    }
  }

// ======== RIPPLE DRAWABLE ======== //

  @Override
  protected void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
    swipeActionTriggerDrawable.draw(canvas);
  }

  @Override
  public void invalidateDrawable(@NonNull Drawable drawable) {
    if (drawable == swipeActionTriggerDrawable) {
      invalidate();
    } else {
      super.invalidateDrawable(drawable);
    }
  }

  @Override
  protected boolean verifyDrawable(@NonNull Drawable who) {
    return swipeActionTriggerDrawable == who || super.verifyDrawable(who);
  }
}
