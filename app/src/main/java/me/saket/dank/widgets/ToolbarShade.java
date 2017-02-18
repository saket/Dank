package me.saket.dank.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.FloatRange;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;

import me.saket.dank.ui.submission.SubmissionFragment;

/**
 * Acts as the background of the Toolbar present in {@link SubmissionFragment} and covers the
 * toolbar's background when the comment list reaches the bottom of the toolbar. Note that right
 * now this View assumes that it'll be located at the top of its parent and the
 * {@link ScrollingRecyclerViewSheet} will be present below the toolbar.
 */
public class ToolbarShade extends View {

    private ValueAnimator shadeFillAnimator;
    private Boolean isToolbarFilled;
    private Float currentFillFactor = 0f;

    public ToolbarShade(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Tracks <var>sheet</var>'s top offset and keeps this View always on top of it.
     * Since {@link ScrollingRecyclerViewSheet} uses translationY changes to scroll, this
     */
    public void syncBottomWithViewTop(ScrollingRecyclerViewSheet sheet) {
        sheet.setOnSheetScrollChangeListener((translationY) -> {
            if (isEnabled()) {
                setTranslationY(translationY);
                toggleFill(translationY <= 0);
            }
        });
    }

    /**
     * When disabled, this View stops scrolling with the View passed in
     * {@link #syncBottomWithViewTop(ScrollingRecyclerViewSheet)} and stays fixed at the top
     * with the toolbar shade fully filled.
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (!enabled) {
            toggleFill(true);
        }
    }

    /**
     * Fill/empty the toolbar's shade.
     */
    private void toggleFill(boolean fill) {
        if (isToolbarFilled != null && isToolbarFilled == fill) {
            return;
        }
        isToolbarFilled = fill;

        Float targetFillFactor = fill ? 1f : 0f;
        if (isLaidOut()) {
            if (shadeFillAnimator != null) {
                shadeFillAnimator.cancel();
            }

            shadeFillAnimator = ValueAnimator.ofFloat(currentFillFactor, targetFillFactor);
            shadeFillAnimator.addUpdateListener(animation -> {
                setShadeFill((Float) animation.getAnimatedValue());
            });
            shadeFillAnimator.setInterpolator(new FastOutSlowInInterpolator());
            shadeFillAnimator.setDuration(150);
            shadeFillAnimator.start();

        } else {
            setShadeFill(targetFillFactor);
        }
    }

    /**
     * Fills the toolbar shade from bottom to top.
     */
    private void setShadeFill(@FloatRange(from = 0, to = 1) Float fillFactor) {
        currentFillFactor = fillFactor;
        invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.clipRect(0, getHeight() - (getHeight() * currentFillFactor), getRight(), getBottom());
        super.draw(canvas);
    }

}
