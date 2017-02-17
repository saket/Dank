package me.saket.dank.widgets;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.widget.ProgressBar;

/**
 * Because {@link ProgressBar#setProgress(int, boolean)} is API 24+ only.
 */
public class AnimatableProgressBar extends ProgressBar {

    private ObjectAnimator progressAnimator;

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

}
