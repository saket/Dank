package me.saket.dank.widgets;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.alexvasilkov.gestures.GestureController;
import com.alexvasilkov.gestures.views.GestureImageView;

/**
 * This wrapper exists so that we can easily change libraries in the future.
 * It has happened once so far and can happen again.
 */
public class ZoomableImageView extends GestureImageView {

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getController().getSettings().setOverzoomFactor(1.3f);
    }

    @Override
    public void setOnClickListener(OnClickListener listener) {
        getController().setOnGesturesListener(new GestureController.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
                listener.onClick(ZoomableImageView.this);
                return true;
            }
        });
    }

    public void setGravity(int gravity) {
        getController().getSettings().setGravity(gravity);
    }

    public int getImageHeight() {
        return getController().getSettings().getImageH();
    }

    public float getZoomedImageHeight() {
        return (float) getImageHeight() * getZoom();
    }

    public float getZoom() {
        return getController().getState().getZoom();
    }

    public boolean canPanUpwardsAnymore() {
        return getController().getState().getY() != 0f;
    }

}
