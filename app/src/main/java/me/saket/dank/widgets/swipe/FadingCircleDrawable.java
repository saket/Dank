package me.saket.dank.widgets.swipe;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.FastOutSlowInInterpolator;

/**
 * Mimics a ripple drawable. Used for indicating that a swipe action has been performed in {@link SwipeableLayout}.
 */
public class FadingCircleDrawable extends Drawable {

  private static final int ANIM_DURATION = 400;
  private static final int MAX_ALPHA = 255 / 4;   // 25% alpha.

  private PointF circleCenter;
  private float circleRadius;
  private Paint paint;
  private AnimatorSet animator;

  public FadingCircleDrawable() {
    circleCenter = new PointF();
    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
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

  public void play(@ColorInt int color, boolean playFromStart) {
    if (animator != null) {
      animator.cancel();
    }
    paint.setColor(color);

    // So we want the circle to animate from a radius equal to this Drawable's height.
    // For this, we'll offset the center and the starting & ending radii by the height.
    if (playFromStart) {
      circleCenter.x = getBounds().left - getBounds().height();
    } else {
      circleCenter.x = getBounds().right + getBounds().height();
    }
    circleCenter.y = getBounds().height() / 2;

    // Adding height to the radii because we add the same height to center-x in draw().
    ObjectAnimator radiusAnimator = ObjectAnimator.ofFloat(this, "radius", getBounds().height(), getBounds().width() + getBounds().height());
    radiusAnimator.setDuration(ANIM_DURATION);

    setAlphaWithoutInvalidate(MAX_ALPHA);
    ObjectAnimator fadeOutAnimator = ObjectAnimator.ofInt(this, "alpha", MAX_ALPHA, 0);
    fadeOutAnimator.setDuration(ANIM_DURATION);
    fadeOutAnimator.setStartDelay(ANIM_DURATION / 2);

    animator = new AnimatorSet();
    animator.setInterpolator(new FastOutSlowInInterpolator());
    animator.playTogether(radiusAnimator, fadeOutAnimator);
    animator.start();
  }
}
