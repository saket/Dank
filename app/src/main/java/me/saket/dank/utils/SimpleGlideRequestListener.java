package me.saket.dank.utils;

import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

/**
 * Stub implementation of {@link RequestListener}.
 */
public abstract class SimpleGlideRequestListener implements RequestListener<String, GlideDrawable> {
    @Override
    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
        return false;
    }

    @Override
    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
        onResourceReady(resource);
        return false;
    }

    public abstract void onResourceReady(GlideDrawable resource);
}
