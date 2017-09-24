package me.saket.dank.widgets.InboxUI;

import android.os.Looper;

import com.jakewharton.rxbinding2.internal.Notification;

import io.reactivex.Observable;

/**
 * Rx bindings for {@link ExpandablePageLayout}.
 */
public class RxExpandablePage {

  public static Observable<Object> streamPreExpansions(ExpandablePageLayout expandablePage) {
    return Observable.create(emitter -> {
      if (!isMainThread()) {
        emitter.onError(new IllegalStateException("Expected to be called on the main thread but was " + Thread.currentThread().getName()));
        return;
      }

      SimpleExpandablePageStateChangeCallbacks listener = new SimpleExpandablePageStateChangeCallbacks() {
        @Override
        public void onPageAboutToExpand(long expandAnimDuration) {
          emitter.onNext(Notification.INSTANCE);
        }
      };
      if (expandablePage.isExpandedOrExpanding()) {
        listener.onPageAboutToExpand(-1);
      }
      expandablePage.addStateChangeCallbacks(listener);
      emitter.setCancellable(() -> expandablePage.removeStateChangeCallbacks(listener));
    });
  }

  public static Observable<Object> streamCollapses(ExpandablePageLayout expandablePage) {
    return Observable.create(emitter -> {
      if (!isMainThread()) {
        emitter.onError(new IllegalStateException("Expected to be called on the main thread but was " + Thread.currentThread().getName()));
        return;
      }

      SimpleExpandablePageStateChangeCallbacks listener = new SimpleExpandablePageStateChangeCallbacks() {
        @Override
        public void onPageCollapsed() {
          emitter.onNext(Notification.INSTANCE);
        }
      };
      if (expandablePage.isCollapsed()) {
        listener.onPageCollapsed();
      }
      expandablePage.addStateChangeCallbacks(listener);
      emitter.setCancellable(() -> expandablePage.removeStateChangeCallbacks(listener));
    });
  }

  private static boolean isMainThread() {
    return Looper.myLooper() == Looper.getMainLooper();
  }
}
