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
public class AnimatableProgressBar extends ProgressBar {

    private ObjectAnimator progressAnimator;
    private boolean visibilityAnimationOngoing;
    private Boolean isVisible;

    public AnimatableProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
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

        setPivotY(0f);
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
