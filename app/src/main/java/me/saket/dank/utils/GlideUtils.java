package me.saket.dank.utils;

import android.widget.ImageView;

import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.target.Target;

import timber.log.Timber;

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
        return new GlideDrawableImageViewTarget(imageView) {
            @Override
            protected void setResource(GlideDrawable resource) {
                super.setResource(resource);
                listener.onImageReady(resource);
            }
        };
    }

    public abstract static class SimpleRequestListener<T,R> implements RequestListener<T,R> {
        @Override
        public boolean onException(Exception e, T model, Target<R> target, boolean isFirstResource) {
            onException(e);
            return false;
        }

        @Override
        public boolean onResourceReady(R resource, T model, Target<R> target, boolean isFromMemoryCache, boolean isFirstResource) {
            onResourceReady(resource);
            return false;
        }

        public void onResourceReady(R resource) {}

        public void onException(Exception e) {
            if (e != null) {
                e.printStackTrace();
            } else {
                Timber.e("Couldn't load resource");
            }
        }
    }

}
