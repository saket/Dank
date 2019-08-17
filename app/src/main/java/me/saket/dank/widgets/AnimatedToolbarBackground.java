package me.saket.dank.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import androidx.annotation.FloatRange;
import android.util.AttributeSet;
import android.view.View;

import me.saket.dank.R;
import me.saket.dank.ui.submission.SubmissionPageLayout;
import me.saket.dank.utils.Animations;

/**
 * Acts as the background of the Toolbar present in {@link SubmissionPageLayout} and covers the
 * toolbar's background when the comment list reaches the bottom of the toolbar. Note that right
 * now this View assumes that it'll be located at the top of its parent and the
 * {@link ScrollingRecyclerViewSheet} will be present below the toolbar.
 */
public class AnimatedToolbarBackground extends View {

  private final boolean clickableWhenFilled;
  private ValueAnimator backgroundFillAnimator;
  private Boolean isToolbarFilled;
  private Float currentFillFactor = 0f;
  private final float onVisibleTranslationZ;
  private boolean syncScrollEnabled;

  public AnimatedToolbarBackground(Context context, AttributeSet attrs) {
    super(context, attrs);
    onVisibleTranslationZ = getTranslationZ();
    setTranslationZ(0f);
    toggleFill(true);
    setClickable(true);

    TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.AnimatedToolbarBackground);
    clickableWhenFilled = attributes.getBoolean(R.styleable.AnimatedToolbarBackground_clickableWhenFilled, false);
    attributes.recycle();
  }

  /**
   * Tracks <var>sheet</var>'s top offset and keeps this View always on top of it.
   * Since {@link ScrollingRecyclerViewSheet} uses translationY changes to scroll, this
   */
  public void syncPositionWithSheet(ScrollingRecyclerViewSheet sheet) {
    sheet.addOnSheetScrollChangeListener((newTranslationY) -> {
      if (syncScrollEnabled && getTranslationY() != newTranslationY) {
        setTranslationY(newTranslationY);
        toggleFill(newTranslationY <= 0);
      }
    });
  }

  /**
   * When disabled, this View stops scrolling with the View passed in
   * {@link #syncPositionWithSheet(ScrollingRecyclerViewSheet)} and stays fixed at the top
   * with the toolbar background fully filled.
   */
  public void setSyncScrollEnabled(boolean enabled) {
    syncScrollEnabled = enabled;

    if (!enabled) {
      setTranslationY(0f);
      toggleFill(true);
    }
  }

  /**
   * Fill/empty the toolbar's background.
   */
  private void toggleFill(boolean fill) {
    if (isToolbarFilled != null && isToolbarFilled == fill) {
      return;
    }
    isToolbarFilled = fill;

    if (clickableWhenFilled) {
      setClickable(isToolbarFilled);
    }

    Float targetFillFactor = fill ? 1f : 0f;
    if (isLaidOut()) {
      if (backgroundFillAnimator != null) {
        backgroundFillAnimator.cancel();
      }

      backgroundFillAnimator = ValueAnimator.ofFloat(currentFillFactor, targetFillFactor);
      backgroundFillAnimator.addUpdateListener(animation -> {
        Float factor = (Float) animation.getAnimatedValue();
        setTranslationZ(onVisibleTranslationZ * factor);
        setBackgroundFill(factor);
      });
      backgroundFillAnimator.setInterpolator(Animations.INTERPOLATOR);
      backgroundFillAnimator.setDuration(150);
      backgroundFillAnimator.start();

    } else {
      setBackgroundFill(targetFillFactor);
      setTranslationZ(onVisibleTranslationZ * targetFillFactor);
    }
  }

  /**
   * Fill the toolbar background from bottom to top.
   */
  private void setBackgroundFill(@FloatRange(from = 0, to = 1) Float fillFactor) {
    currentFillFactor = fillFactor;
    invalidate();
  }

  @Override
  public void draw(Canvas canvas) {
    canvas.clipRect(0, getHeight() - (getHeight() * currentFillFactor), getRight(), getBottom());
    super.draw(canvas);
  }
}
