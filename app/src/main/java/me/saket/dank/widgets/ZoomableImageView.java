package me.saket.dank.widgets;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.alexvasilkov.gestures.GestureController;
import com.alexvasilkov.gestures.State;
import com.alexvasilkov.gestures.views.GestureImageView;

/**
 * This wrapper exists so that we can easily change libraries in the future.
 * It has happened once so far and can happen again.
 */
public class ZoomableImageView extends GestureImageView {

    private GestureDetector gestureDetector;

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        getController().setOnGesturesListener(new GestureController.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
                performClick();
                return true;
            }
        });

        // Bug workarounds: GestureImageView doesn't request parent ViewGroups to stop intercepting touch
        // events when it starts consuming them to zoom.
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                getParent().requestDisallowInterceptTouchEvent(true);
                return super.onDoubleTapEvent(e);
            }
        });

        getController().addOnStateChangeListener(new GestureController.OnStateChangeListener() {
            public float lastZoom = -1;

            @Override
            public void onStateChanged(State state) {
                if (lastZoom == -1) {
                    lastZoom = state.getZoom();
                }

                // Overscroll only when zooming in.
                boolean isZoomingIn = state.getZoom() > lastZoom;
                if (isZoomingIn) {
                    getController().getSettings().setOverzoomFactor(2f);
                } else if (state.getZoom() < 1f) {
                    getController().getSettings().setOverzoomFactor(1f);
                }
            }

            @Override
            public void onStateReset(State oldState, State newState) {

            }
        });
    }

    public void setGravity(int gravity) {
        getController().getSettings().setGravity(gravity);
    }

    public int getImageHeight() {
        return getController().getSettings().getImageH();
    }

    /**
     * Calculate height of the image that is currently visible.
     */
    public float getVisibleZoomedImageHeight() {
        float v = ((float) getImageHeight() * getZoom());

        // Subtract the portion that has gone outside limits due to zooming in, because they are
        // longer visible. This currently does not include the bottom offset because it's not
        // needed right now.
        float heightNotVisible = getController().getState().getY();
        if (heightNotVisible < 0) {
            v += heightNotVisible;
        }

        return v;
    }

    public float getZoom() {
        return getController().getState().getZoom();
    }

    /**
     * Whether the image can be panned anymore vertically, upwards or downwards depending upon <var>upwardPan</var>.
     */
    public boolean canPanVertically(boolean upwardPan) {
        float imageY = getController().getState().getY();
        if (upwardPan) {
            return imageY != 0;
        } else {
            return getHeight() - getVisibleZoomedImageHeight() != imageY;
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        gestureDetector.onTouchEvent(event);

        if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() == 2) {
            // Two-finger zoom is probably going to start. Disallow parent from intercepting this gesture.
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        return super.onTouchEvent(event);
    }

}
