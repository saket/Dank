package me.saket.dank.utils.glide;

import androidx.annotation.Nullable;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;
import timber.log.Timber;

/**
 * Utility methods related to Glide.
 */
public class GlideUtils {

  public static class LambdaRequestListener<R> implements RequestListener<R> {
    private final Consumer<R> resourceConsumer;
    private final Consumer<Exception> errorConsumer;

    public LambdaRequestListener(Consumer<R> resource, Consumer<Exception> error) {
      this.resourceConsumer = resource;
      this.errorConsumer = error;
    }

    @Override
    public final boolean onResourceReady(R resource, Object model, Target<R> target, DataSource dataSource, boolean isFirstResource) {
      try {
        resourceConsumer.accept(resource);
      } catch (Exception e) {
        throw Exceptions.propagate(e);
      }
      return false;
    }

    @Override
    public final boolean onLoadFailed(@Nullable GlideException e, Object model, Target<R> target, boolean isFirstResource) {
      try {
        errorConsumer.accept(e);
      } catch (Exception anotherE) {
        throw Exceptions.propagate(anotherE);
      }
      return false;
    }
  }

  public abstract static class SimpleRequestListener<R> implements RequestListener<R> {
    @Override
    public final boolean onLoadFailed(@Nullable GlideException e, Object model, Target<R> target, boolean isFirstResource) {
      onLoadFailed(e);
      return false;
    }

    @Override
    public final boolean onResourceReady(R resource, Object model, Target<R> target, DataSource dataSource, boolean isFirstResource) {
      onResourceReady(resource);
      return false;
    }

    public void onResourceReady(R resource) {}

    public void onLoadFailed(@Nullable Exception e) {
      if (e != null) {
        e.printStackTrace();
      } else {
        Timber.e("Couldn't load resourceConsumer");
      }
    }
  }
}
