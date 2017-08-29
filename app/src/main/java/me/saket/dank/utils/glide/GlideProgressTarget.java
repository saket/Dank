package me.saket.dank.utils.glide;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

public abstract class GlideProgressTarget<T, Z> extends GlideWrappingTarget<Z> implements GlideOkHttpProgressModule.UiProgressListener {

  private T model;
  private boolean ignoreProgress = true;

  public GlideProgressTarget(Target<Z> target) {
    this(null, target);
  }

  public GlideProgressTarget(T model, Target<Z> target) {
    super(target);
    this.model = model;
  }

  public final T getModel() {
    return model;
  }

  public final void setModel(Context context, T model) {
    Glide.with(context).clear(this); // indirectly calls cleanup
    this.model = model;
  }

  /**
   * Convert a model into an Url string that is used to match up the OkHttp requests. For explicit
   * {@link GlideUrl GlideUrl} loads this needs to return {@link GlideUrl#toStringUrl toStringUrl}. For custom
   * models do the same as your {@link BaseGlideUrlLoader BaseGlideUrlLoader} does.
   *
   * @param model return the representation of the given model, DO NOT use {@link #getModel()} inside this method.
   * @return a stable Url representation of the model, otherwise the progress reporting won't work
   */
  protected String toUrlString(T model) {
    return String.valueOf(model);
  }

  @Override
  public float getGranularityPercentage() {
    return 1.0f;
  }

  @Override
  public void onProgress(long bytesRead, long expectedBytes) {
    if (ignoreProgress) {
      return;
    }
    if (expectedBytes == Long.MAX_VALUE) {
      onConnecting();
    } else if (bytesRead == expectedBytes) {
      onDownloaded();
    } else {
      onDownloading(bytesRead, expectedBytes);
    }
  }

  /**
   * Called when the Glide load has started.
   * At this time it is not known if the Glide will even go and use the network to fetch the image.
   */
  protected abstract void onConnecting();

  /**
   * Called when there's any progress on the download; not called when loading from cache.
   * At this time we know how many bytes have been transferred through the wire.
   */
  protected abstract void onDownloading(long bytesRead, long expectedBytes);

  /**
   * Called when the bytes downloaded reach the length reported by the server; not called when loading from cache.
   * At this time it is fairly certain, that Glide either finished reading the stream.
   * This means that the image was either already decoded or saved the network stream to cache.
   * In the latter case there's more work to do: decode the image from cache and transform.
   * These cannot be listened to for progress so it's unsure how fast they'll be, best to show indeterminate progress.
   */
  protected abstract void onDownloaded();

  /**
   * Called when the Glide load has finished either by successfully loading the image or failing to load or cancelled.
   * In any case the best is to hide/reset any progress displays.
   */
  protected abstract void onDelivered();

  private void start() {
    GlideOkHttpProgressModule.expect(toUrlString(model), this);
    ignoreProgress = false;
    onProgress(0, Long.MAX_VALUE);
  }

  private void cleanup() {
    ignoreProgress = true;
    T model = this.model; // save in case it gets modified
    onDelivered();
    GlideOkHttpProgressModule.forget(toUrlString(model));
    this.model = null;
  }

  @Override
  public void onLoadStarted(Drawable placeholder) {
    super.onLoadStarted(placeholder);
    start();
  }

  @Override
  public void onResourceReady(Z resource, Transition<? super Z> transition) {
    cleanup();
    super.onResourceReady(resource, transition);
  }

  @Override
  public void onLoadFailed(Drawable errorDrawable) {
    cleanup();
    super.onLoadFailed(errorDrawable);
  }

  @Override
  public void onLoadCleared(Drawable placeholder) {
    cleanup();
    super.onLoadCleared(placeholder);
  }
}
