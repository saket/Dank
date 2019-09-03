package me.saket.dank.widgets.InboxUI;

import android.os.Looper;

import androidx.annotation.CheckResult;

import io.reactivex.Observable;

/**
 * Rx bindings for {@link ExpandablePageLayout}.
 */
public class RxExpandablePage {

  @CheckResult
  public static Observable<ExpandablePageLayout.PageState> stateChanges(ExpandablePageLayout page) {
    return Observable.create(emitter -> {
      if (!isMainThread()) {
        throw new IllegalStateException("Expected to be called on the main thread but was " + Thread.currentThread().getName());
      }

      ExpandablePageLayout.StateChangeCallbacks listener = new ExpandablePageLayout.StateChangeCallbacks() {
        @Override
        public void onPageAboutToExpand(long expandAnimDuration) {
          emitter.onNext(ExpandablePageLayout.PageState.EXPANDING);
        }

        @Override
        public void onPageExpanded() {
          emitter.onNext(ExpandablePageLayout.PageState.EXPANDED);
        }

        @Override
        public void onPageAboutToCollapse(long collapseAnimDuration) {
          emitter.onNext(ExpandablePageLayout.PageState.COLLAPSING);
        }

        @Override
        public void onPageCollapsed() {
          emitter.onNext(ExpandablePageLayout.PageState.COLLAPSED);
        }
      };

      emitter.onNext(page.getCurrentState());
      page.addStateChangeCallbacks(listener);
      emitter.setCancellable(() -> page.removeStateChangeCallbacks(listener));
    });
  }

  private static boolean isMainThread() {
    return Looper.myLooper() == Looper.getMainLooper();
  }
}
