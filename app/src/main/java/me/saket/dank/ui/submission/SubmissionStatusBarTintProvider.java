package me.saket.dank.ui.submission;

import android.graphics.Bitmap;
import android.support.annotation.CheckResult;

import io.reactivex.Observable;
import me.saket.dank.data.StatusBarTint;
import me.saket.dank.utils.StatusBarTintProvider;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.PullToCollapseListener;
import me.saket.dank.widgets.InboxUI.SimpleExpandablePageStateChangeCallbacks;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;

class SubmissionStatusBarTintProvider {

  private final StatusBarTintProvider statusBarTintProvider;
  private final int defaultStatusBarColor;

  /**
   * @param statusBarHeight Used with Palette for setting the photo's region from where the color will be extracted.
   */
  SubmissionStatusBarTintProvider(int defaultStatusBarColor, int statusBarHeight, int displayWidth) {
    this.defaultStatusBarColor = defaultStatusBarColor;
    this.statusBarTintProvider = new StatusBarTintProvider(this.defaultStatusBarColor, statusBarHeight, displayWidth);
  }

  @CheckResult
  public Observable<StatusBarTint> streamStatusBarTintColor(Observable<Bitmap> contentBitmapStream, ExpandablePageLayout expandablePageLayout,
      ScrollingRecyclerViewSheet commentListParentSheet)
  {
    StatusBarTint defaultTint = StatusBarTint.create(defaultStatusBarColor, true /* isDark */);
    return contentBitmapStream
        .switchMapSingle(bitmap -> statusBarTintProvider.generateTint(bitmap))
        .startWith(defaultTint)
        .flatMap(statusBarTint -> Observable.combineLatest(
            streamIsContentPartiallyOrFullyVisible(commentListParentSheet).distinctUntilChanged(),
            streamIsPageFullyVisible(expandablePageLayout).distinctUntilChanged(),
            (isContentPartiallyOrFullyVisible, isPageFullyVisible) -> {
              if (!isContentPartiallyOrFullyVisible) {
                // When the comment list reaches the top, the toolbar's background is filled to make it opaque.
                // We'll delay the tint transition to coordinate with that animation.
                return defaultTint.withDelayedTransition(true /* delayedTransition */);
              } else {
                return isPageFullyVisible ? statusBarTint : defaultTint;
              }
            }
        ))
        .distinctUntilChanged((prevTint, newTint) -> prevTint.color() == newTint.color());
  }

  private Observable<Boolean> streamIsPageFullyVisible(ExpandablePageLayout expandablePageLayout) {
    return Observable.create(emitter -> {
      PullToCollapseListener.OnPullListener pullListener = new PullToCollapseListener.OnPullListener() {
        @Override
        public void onPull(float deltaY, float currentTranslationY, boolean upwardPull, boolean deltaUpwardPull, boolean collapseEligible) {
          //Timber.i("collapseEligible: %s", collapseEligible);
          emitter.onNext(!collapseEligible);
        }

        @Override
        public void onRelease(boolean collapseEligible) {
          emitter.onNext(!collapseEligible);
        }
      };
      SimpleExpandablePageStateChangeCallbacks stateChangeCallbacks = new SimpleExpandablePageStateChangeCallbacks() {
        @Override
        public void onPageExpanded() {
          //Timber.i("Page expanded. Showing tint.");
          emitter.onNext(true);
        }

        @Override
        public void onPageAboutToCollapse(long collapseAnimDuration) {
          //Timber.i("Page about to collapse. Hiding tint.");
          emitter.onNext(false);
        }
      };

      expandablePageLayout.addOnPullListener(pullListener);
      expandablePageLayout.addStateChangeCallbacks(stateChangeCallbacks);

      emitter.setCancellable(() -> {
        expandablePageLayout.removeOnPullListener(pullListener);
        expandablePageLayout.removeStateChangeCallbacks(stateChangeCallbacks);
      });

      // Initial callbacks
      if (expandablePageLayout.isExpanded()) {
        stateChangeCallbacks.onPageExpanded();
      }
    });
  }

  private Observable<Boolean> streamIsContentPartiallyOrFullyVisible(ScrollingRecyclerViewSheet commentListParentSheet) {
    return Observable.create(emitter -> {
      ScrollingRecyclerViewSheet.SheetScrollChangeListener scrollChangeListener = newScrollY -> {
        emitter.onNext(newScrollY > 0f);
      };
      commentListParentSheet.addOnSheetScrollChangeListener(scrollChangeListener);
      emitter.setCancellable(() -> commentListParentSheet.removeOnSheetScrollChangeListener(scrollChangeListener));

      // Initial callbacks
      scrollChangeListener.onScrollChange(commentListParentSheet.currentScrollY());
    });
  }
}
