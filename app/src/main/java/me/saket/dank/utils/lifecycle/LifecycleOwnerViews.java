package me.saket.dank.utils.lifecycle;

import android.support.annotation.CheckResult;
import android.view.View;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

/**
 * This class exists instead of just {@link Streams} to keep it consistent with
 * {@link LifecycleOwnerActivity}, {@link LifecycleOwnerFragment}, etc.
 */
public class LifecycleOwnerViews {

  /**
   * @param parentLifecycleOwner From parent Activity or fragment.
   */
  public static Streams create(View view, LifecycleOwner parentLifecycleOwner) {
    return new Streams(view, parentLifecycleOwner.lifecycle());
  }

  public static class Streams extends ForwardingLifecycleStreams {

    private Relay<Object> viewAttaches = PublishRelay.create();
    private Relay<Object> viewDetaches = PublishRelay.create();

    public Streams(View view, LifecycleStreams delegate) {
      super(delegate);

      view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
        @Override
        public void onViewAttachedToWindow(View v) {
          viewAttaches.accept(LifecycleStreams.NOTHING);
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
          viewDetaches.accept(LifecycleStreams.NOTHING);
        }
      });
    }

    @CheckResult
    public Relay<Object> viewAttaches() {
      return viewAttaches;
    }

    @CheckResult
    public Relay<Object> viewDetaches() {
      return viewDetaches;
    }
  }
}
