package me.saket.dank.widgets.swipe;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;

import me.saket.dank.utils.Animations;

/**
 * Mimics a ripple drawable. Used for indicating that a swipe action has been performed in {@link SwipeableLayout}.
 */
public class SwipeTriggerRippleDrawable extends Drawable {

  private static final int ANIM_DURATION = 400;
  private static final int MAX_ALPHA = 255 / 4;   // 25% alpha.

  private PointF circleCenter;
  private float circleRadius;
  private Paint paint;
  private AnimatorSet animator;

  public enum RippleType {
    /**
     * Ripple will expand to fill the entire layout.
     */
    REGISTER,

    /**
     * Ripple will start in expanded mode and retract to its center.
     * Used when undo-ing an existing action (e.g., upvote).
     */
    UNDO,
  }

  public SwipeTriggerRippleDrawable() {
    circleCenter = new PointF();
    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  }

  @Override
  public void draw(Canvas canvas) {
    canvas.drawCircle(circleCenter.x, circleCenter.y, circleRadius, paint);
  }

  @Override
  public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
    setAlphaWithoutInvalidate(alpha);
    invalidateSelf();
  }

  @SuppressWarnings("unused")
  protected void setRadius(float radius) {
    circleRadius = radius;
    invalidateSelf();
  }

  public void setAlphaWithoutInvalidate(@IntRange(from = 0, to = 255) int alpha) {
    paint.setAlpha(alpha);
  }

  @Override
  public int getAlpha() {
    return paint.getAlpha();
  }

  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {
    paint.setColorFilter(colorFilter);
    invalidateSelf();
  }

  @Override
  public int getOpacity() {
    return PixelFormat.OPAQUE;
  }

  public void play(@ColorInt int color, SwipeDirection rippleDirection, RippleType swipeRippleType) {
    if (animator != null) {
      animator.cancel();
    }
    paint.setColor(color);

    // Adding height to the radii because we add the same height to center-x in draw().
    boolean isUndoRipple = swipeRippleType == RippleType.UNDO;
    int startRadius = isUndoRipple ? getBounds().width() + getBounds().height() : getBounds().height();
    int endRadius = isUndoRipple ? 0 : getBounds().width() + getBounds().height();

    long animationDuration = ANIM_DURATION;
    if (isUndoRipple) {
      // Reverse animation looks faster in speed, so slow it down further.
      animationDuration *= 1.75;
    }

    ObjectAnimator radiusAnimator = ObjectAnimator.ofFloat(this, "radius", startRadius, endRadius);
    radiusAnimator.setDuration(animationDuration);
    radiusAnimator.addUpdateListener(animation -> {
      // So we want the circle to animate from a radius equal to this Drawable's height.
      // For this, we'll offset the center and the starting & ending radii by the height.
      if (rippleDirection == SwipeDirection.START_TO_END) {
        circleCenter.x = getBounds().left - getBounds().height();
      } else {
        circleCenter.x = getBounds().right + getBounds().height();
      }
      circleCenter.y = getBounds().height() / 2;
    });

    setAlphaWithoutInvalidate(MAX_ALPHA);
    ObjectAnimator fadeOutAnimator = ObjectAnimator.ofInt(this, "alpha", MAX_ALPHA, 0);
    fadeOutAnimator.setDuration(animationDuration);
    fadeOutAnimator.setStartDelay(isUndoRipple ? 0 : animationDuration / 2);

    animator = new AnimatorSet();
    animator.setInterpolator(Animations.INTERPOLATOR);
    animator.playTogether(radiusAnimator, fadeOutAnimator);
    animator.start();
  }
}
