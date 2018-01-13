package me.saket.dank.widgets.InboxUI;

import android.os.Looper;
import android.support.annotation.CheckResult;

import com.jakewharton.rxbinding2.internal.Notification;

import io.reactivex.Observable;

/**
 * Rx bindings for {@link ExpandablePageLayout}.
 */
public class RxExpandablePage {

  @CheckResult
  public static Observable<Object> preExpansions(ExpandablePageLayout expandablePage) {
    return Observable.create(emitter -> {
      if (!isMainThread()) {
        throw new IllegalStateException("Expected to be called on the main thread but was " + Thread.currentThread().getName());
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

  @CheckResult
  public static Observable<Object> collapses(ExpandablePageLayout expandablePage) {
    return Observable.create(emitter -> {
      if (!isMainThread()) {
        throw new IllegalStateException("Expected to be called on the main thread but was " + Thread.currentThread().getName());
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

//  @CheckResult
//  public static Observable<ExpandablePageLayout.PageState> pageStateChanges(ExpandablePageLayout page) {
//    return Observable.create(emitter -> {
//      if (!isMainThread()) {
//        throw new IllegalStateException("Expected to be called on the main thread but was " + Thread.currentThread().getName());
//      }
//
//      ExpandablePageLayout.StateChangeCallbacks listener = new ExpandablePageLayout.StateChangeCallbacks() {
//        @Override
//        public void onPageAboutToExpand(long expandAnimDuration) {
//          emitter.onNext(ExpandablePageLayout.PageState.EXPANDING);
//        }
//
//        @Override
//        public void onPageExpanded() {
//          emitter.onNext(ExpandablePageLayout.PageState.EXPANDED);
//        }
//
//        @Override
//        public void onPageAboutToCollapse(long collapseAnimDuration) {
//          emitter.onNext(ExpandablePageLayout.PageState.COLLAPSING);
//        }
//
//        @Override
//        public void onPageCollapsed() {
//          emitter.onNext(ExpandablePageLayout.PageState.COLLAPSED);
//        }
//      };
//
//      emitter.onNext(page.getCurrentState());
//      page.addStateChangeCallbacks(listener);
//      emitter.setCancellable(() -> page.removeStateChangeCallbacks(listener));
//    });
//  }

  private static boolean isMainThread() {
    return Looper.myLooper() == Looper.getMainLooper();
  }
}
