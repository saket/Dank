package me.saket.dank.utils.glide;

import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.DrawableImageViewTarget;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.target.Target;

import timber.log.Timber;

/**
 * Utility methods related to Glide.
 */
public class GlideUtils {

  public interface OnImageResourceReadyListener {
    void onImageReady(Drawable drawable);
  }

  /**
   * This method exists to reduce line indentation by one level.
   */
  public static ImageViewTarget<Drawable> simpleImageViewTarget(ImageView imageView, OnImageResourceReadyListener listener) {
    return new DrawableImageViewTarget(imageView) {
      @Override
      protected void setResource(Drawable resource) {
        super.setResource(resource);
        listener.onImageReady(resource);
      }
    };
  }

  public abstract static class SimpleRequestListener<R> implements RequestListener<R> {
    @Override
    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<R> target, boolean isFirstResource) {
      onLoadFailed(e);
      return false;
    }

    @Override
    public boolean onResourceReady(R resource, Object model, Target<R> target, DataSource dataSource, boolean isFirstResource) {
      onResourceReady(resource);
      return false;
    }

    public void onResourceReady(R resource) {}

    public void onLoadFailed(Exception e) {
      if (e != null) {
        e.printStackTrace();
      } else {
        Timber.e("Couldn't load resource");
      }
    }
  }
}
