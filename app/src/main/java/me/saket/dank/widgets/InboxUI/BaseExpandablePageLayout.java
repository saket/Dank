package me.saket.dank.widgets.InboxUI;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Outline;
import android.graphics.Rect;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.RelativeLayout;

/**
 * Handles change in dimensions. This class exists because animating the dimensions (using an
 * ObjectAnimator) of a complex layout isn't very smooth.
 */
public abstract class BaseExpandablePageLayout extends RelativeLayout {

  public static final long DEFAULT_ANIM_DURATION = 250;
  private static TimeInterpolator ANIM_INTERPOLATOR = new FastOutSlowInInterpolator();

  private final Rect clippedDimensionRect = new Rect();
  private ValueAnimator dimensionAnimator;
  private boolean isFullyVisible;
  private long animationDuration = DEFAULT_ANIM_DURATION;

  public BaseExpandablePageLayout(Context context) {
    super(context);
    init();
  }

  public BaseExpandablePageLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    setOutlineProvider(new ViewOutlineProvider() {
      @Override
      public void getOutline(View view, Outline outline) {
        outline.setRect(0, 0, clippedDimensionRect.width(), clippedDimensionRect.height());
      }
    });
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    if (isFullyVisible) {
      setClippedDimensions(w, h);
    }
  }

  public void animateDimensions(Integer toWidth, Integer toHeight) {
    cancelOngoingClipAnimation();

    final Float fromWidth = getClippedWidth();
    final Float fromHeight = getClippedHeight();

    dimensionAnimator = ObjectAnimator.ofFloat(0f, 1f);
    dimensionAnimator.addUpdateListener(animation -> {
      final Float scale = (Float) animation.getAnimatedValue();
      final int newWidth = (int) ((toWidth - fromWidth) * scale + fromWidth);
      final int newHeight = (int) ((toHeight - fromHeight) * scale + fromHeight);
      setClippedDimensions(newWidth, newHeight);
    });

    dimensionAnimator.setDuration(getAnimationDurationMillis());
    dimensionAnimator.setInterpolator(getAnimationInterpolator());
    dimensionAnimator.setStartDelay(InboxRecyclerView.getAnimationStartDelay());
    dimensionAnimator.start();
  }

// ======== GETTERS & SETTERS ======== //

  public void setClippedDimensions(int newClippedWidth, int newClippedHeight) {
    isFullyVisible = newClippedWidth > 0 && newClippedHeight > 0 && newClippedWidth == getWidth() && newClippedHeight == getHeight();

    clippedDimensionRect.right = newClippedWidth;
    clippedDimensionRect.bottom = newClippedHeight;

    setClipBounds(new Rect(clippedDimensionRect.left, clippedDimensionRect.top, clippedDimensionRect.right, clippedDimensionRect.bottom));
    invalidateOutline();
  }

  /**
   * Immediately resets the clipping so that the whole layout becomes visible
   */
  public void resetClipping() {
    setClippedDimensions(getWidth(), getHeight());
  }

  protected float getClippedWidth() {
    return clippedDimensionRect.width();
  }

  protected float getClippedHeight() {
    return clippedDimensionRect.height();
  }

  public Rect getClippedRect() {
    return clippedDimensionRect;
  }

  protected TimeInterpolator getAnimationInterpolator() {
    return ANIM_INTERPOLATOR;
  }

  public void setAnimationDuration(long duration) {
    animationDuration = duration;
  }

  public long getAnimationDurationMillis() {
    return animationDuration;
  }

  protected void cancelOngoingClipAnimation() {
    if (dimensionAnimator != null) {
      dimensionAnimator.cancel();
    }
  }
}
