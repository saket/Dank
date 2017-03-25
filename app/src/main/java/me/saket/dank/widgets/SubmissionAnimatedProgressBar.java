package me.saket.dank.widgets;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;

/**
 * A ProgressBar that uses height animation for hiding/showing + animates changes in determinate progress
 * (because {@link ProgressBar#setProgress(int, boolean)} is API 24+ only).
 */
public class SubmissionAnimatedProgressBar extends ProgressBar {

    private ObjectAnimator progressAnimator;
    private boolean visibilityAnimationOngoing;
    private Boolean isVisible;
    private boolean syncScrollEnabled;

    public SubmissionAnimatedProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Tracks <var>sheet</var>'s top offset and keeps this View always on top of it.
     * Since {@link ScrollingRecyclerViewSheet} uses translationY changes to scroll, this
     */
    public void syncPositionWithSheet(ScrollingRecyclerViewSheet sheet) {
        sheet.addOnSheetScrollChangeListener((newTranslationY) -> {
            if (syncScrollEnabled && getTranslationY() != newTranslationY) {
                setTranslationY(newTranslationY);
            }
        });
    }

    /**
     * When disabled, this View stops scrolling with the sheet passed in {@link
     * #syncPositionWithSheet(ScrollingRecyclerViewSheet)} and stays fixed below the toolbar.
     */
    public void setSyncScrollEnabled(boolean enabled) {
        syncScrollEnabled = enabled;
    }

    public void setProgressWithAnimation(int toProgress) {
        cancelProgressAnimation();

        progressAnimator = ObjectAnimator.ofInt(this, "progress", getProgress(), toProgress);
        progressAnimator.setInterpolator(new FastOutSlowInInterpolator());
        progressAnimator.setDuration(400);
        progressAnimator.start();
    }

    private void cancelProgressAnimation() {
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
    }

    @Override
    public synchronized void setIndeterminate(boolean indeterminate) {
        cancelProgressAnimation();
        super.setIndeterminate(indeterminate);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        new Exception("Use setVisible() instead").printStackTrace();
    }

    public void show() {
        setVisible(true);
    }

    public void hide() {
        setVisible(false);
    }

    public void setVisible(boolean visible) {
        // Ignore if already visible/hidden.
        if (isVisible != null && isVisible == visible) {
            return;
        }
        isVisible = visible;

        setPivotY(getHeight() * 4 / 10);    // This value of 40% calculated using trial and error. Change this if needed.
        animate()
                .scaleY(visible ? 1f : 0f)
                .setStartDelay(visibilityAnimationOngoing ? 100 : 0)
                .setInterpolator(new DecelerateInterpolator())
                .setDuration(400)
                .withStartAction(() -> visibilityAnimationOngoing = true)
                .withEndAction(() -> visibilityAnimationOngoing = false)
                .start();
    }

}
