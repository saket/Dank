package me.saket.dank.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.target.ImageViewTarget;

/**
 * Utility methods related to Glide.
 */
public class GlideUtils {

    public interface OnImageResourceReadyListener {
        void onImageReady(GlideDrawable drawable);
    }

    /**
     * This method exists to reduce line indentation by one level.
     */
    public static ImageViewTarget<GlideDrawable> simpleImageViewTarget(ImageView imageView, OnImageResourceReadyListener listener) {
        return new ImageViewTarget<GlideDrawable>(imageView) {
            @Override
            protected void setResource(GlideDrawable resource) {
                listener.onImageReady(resource);
            }
        };
    }

}
