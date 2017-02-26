package me.saket.dank.widgets.InboxUI;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.RelativeLayout;

/**
 * Handles change in dimensions. This class exists because animating the dimensions (using an
 * ObjectAnimator) of a complex layout isn't very smooth.
 */
public abstract class BaseExpandablePageLayout extends RelativeLayout {

    private Path path;
    private RectF clippedDimensionRect;
    private ValueAnimator dimensionAnimator;
    private boolean isClipped;

    public BaseExpandablePageLayout(Context context) {
        super(context);
        init();
    }

    public BaseExpandablePageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BaseExpandablePageLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BaseExpandablePageLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        clippedDimensionRect = new RectF();
        path = new Path();

        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRect(0, 0, ((int) clippedDimensionRect.width()), (int) clippedDimensionRect.height());
            }
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (!isClipped) {
            animateDimensions(w, h);
        } else {
            setClippedDimensions(w, h);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.clipPath(path);
        super.draw(canvas);
    }

    protected void animateDimensions(Integer toWidth, Integer toHeight) {
        if (dimensionAnimator != null) {
            dimensionAnimator.cancel();
        }

        final Float fromWidth = getClippedWidth();
        final Float fromHeight = getClippedHeight();

        dimensionAnimator = ObjectAnimator.ofFloat(0f, 1f);
        dimensionAnimator.addUpdateListener(animation -> {
            final Float scale = (Float) animation.getAnimatedValue();
            final Float newWidth = ((toWidth - fromWidth) * scale + fromWidth);
            final Float newHeight = ((toHeight - fromHeight) * scale + fromHeight);
            setClippedDimensions(newWidth, newHeight);
        });

        dimensionAnimator.setDuration(getAnimationDuration());
        dimensionAnimator.setInterpolator(getAnimationInterpolator());
        dimensionAnimator.setStartDelay(InboxRecyclerView.ANIM_START_DELAY);
        dimensionAnimator.start();
    }

// ======== GETTERS & SETTERS ======== //

    // This method exists so that animateDimensions() can animate dimensions without
    // thrashing Float objects (due to auto-boxing) on every frame.
    public void setClippedDimensions(float newClippedWidth, float newClippedHeight) {
        setClippedDimensions((Float) newClippedWidth, (Float) newClippedHeight);
    }

    public void setClippedDimensions(Float newClippedWidth, Float newClippedHeight) {
        isClipped = newClippedWidth != getWidth() || newClippedHeight != getHeight();

        clippedDimensionRect.right = newClippedWidth;
        clippedDimensionRect.bottom = newClippedHeight;

        path.reset();
        path.addRect(clippedDimensionRect, Path.Direction.CW);

        postInvalidate();
        invalidateOutline();
    }

    /**
     * Immediately resets the clipping so that the whole layout becomes visible
     */
    public void resetClipping() {
        setClippedDimensions((float) getWidth(), (float) getHeight());
    }

    /**
     * When this is false, the entire page is visible.
     */
    public boolean isClipped() {
        return isClipped;
    }

    protected float getClippedWidth() {
        return clippedDimensionRect.width();
    }

    protected float getClippedHeight() {
        return clippedDimensionRect.height();
    }

    public RectF getClippedRect() {
        return clippedDimensionRect;
    }

    protected TimeInterpolator getAnimationInterpolator() {
        return InboxRecyclerView.ANIM_INTERPOLATOR_EXPAND;
    }

    protected long getAnimationDuration() {
        return InboxRecyclerView.ANIM_DURATION_EXPAND;
    }

}
