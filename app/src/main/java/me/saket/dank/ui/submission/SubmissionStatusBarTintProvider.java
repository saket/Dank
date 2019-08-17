package me.saket.dank.ui.submission;

import android.graphics.Bitmap;
import androidx.annotation.CheckResult;

import io.reactivex.Observable;
import io.reactivex.Single;
import me.saket.dank.data.StatusBarTint;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.StatusBarTintProvider;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.PullToCollapseListener;
import me.saket.dank.widgets.InboxUI.SimpleExpandablePageStateChangeCallbacks;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import timber.log.Timber;

class SubmissionStatusBarTintProvider {

  private final StatusBarTintProvider statusBarTintProvider;
  private final int defaultStatusBarColor;

  private enum SubmissionPageState {
    BEING_PULLED_BUT_WILL_SNAP_BACK,
    BEING_PULLED_AND_ELIGIBLE_FOR_COLLAPSE,
    EXPANDED,
    ABOUT_TO_COLLAPSE,
    COLLAPSED,
  }

  /**
   * @param statusBarHeight Used with Palette for setting the photo's region from where the color will be extracted.
   */
  SubmissionStatusBarTintProvider(int defaultStatusBarColor, int statusBarHeight, int displayWidth) {
    this.defaultStatusBarColor = defaultStatusBarColor;
    this.statusBarTintProvider = new StatusBarTintProvider(this.defaultStatusBarColor, statusBarHeight, displayWidth);
  }

  // TODO v2: This doesn't work with SubmissionPageLayoutActivity. Pull-to-collapse listener uses Activity's root page instead of submission page.
  @CheckResult
  public Observable<StatusBarTint> streamStatusBarTintColor(
      Observable<Optional<Bitmap>> contentBitmapStream,
      ExpandablePageLayout expandablePageLayout,
      ScrollingRecyclerViewSheet commentListParentSheet)
  {
    StatusBarTint defaultTint = StatusBarTint.create(defaultStatusBarColor, true);
    return contentBitmapStream
        .switchMapSingle(optionalBitmap -> {
          if (optionalBitmap.isPresent()) {
            return statusBarTintProvider.generateTint(optionalBitmap.get());
          } else {
            Timber.i("Empty image. Returning default tint.");
            return Single.just(defaultTint);
          }
        })
        .startWith(defaultTint)
        .switchMap(statusBarTint -> {
          Observable<SubmissionPageState> pageStateStream = streamPageState(expandablePageLayout).distinctUntilChanged();
          Observable<Boolean> contentVisibilityStream = streamContentVisibilityState(commentListParentSheet).distinctUntilChanged();

          Observable<StatusBarTint> filteredTintStream = Observable.combineLatest(contentVisibilityStream, pageStateStream,
              (isContentPartiallyOrFullyVisible, submissionPageState) -> {
                if (!isContentPartiallyOrFullyVisible) {
                  // When the comment list reaches the top, the toolbar's background is filled to make it opaque.
                  // We'll delay the tint transition to coordinate with that animation.
                  return defaultTint.withDelayedTransition(true /* delayedTransition */);

                } else {
                  switch (submissionPageState) {
                    case BEING_PULLED_BUT_WILL_SNAP_BACK:
                    case EXPANDED:
                      return statusBarTint;

                    case BEING_PULLED_AND_ELIGIBLE_FOR_COLLAPSE:
                    case ABOUT_TO_COLLAPSE:
                    case COLLAPSED:
                      return defaultTint;

                    default:
                      throw new UnsupportedOperationException("Unknown state: " + submissionPageState);
                  }
                }
              }
          );

          // Short-circuit this switchMap chain if the page collapses. We'll resume when a new content is received.
          return filteredTintStream.takeUntil(pageStateStream.filter(state -> state == SubmissionPageState.COLLAPSED));
        })
        .distinctUntilChanged((prevTint, newTint) -> prevTint.color() == newTint.color());
  }

  private Observable<SubmissionPageState> streamPageState(ExpandablePageLayout expandablePageLayout) {
    return Observable.create(emitter -> {
      PullToCollapseListener.OnPullListener pullListener = new PullToCollapseListener.OnPullListener() {
        @Override
        public void onPull(float deltaY, float currentTranslationY, boolean upwardPull, boolean deltaUpwardPull, boolean collapseEligible) {
          emitter.onNext(collapseEligible
              ? SubmissionPageState.BEING_PULLED_AND_ELIGIBLE_FOR_COLLAPSE
              : SubmissionPageState.BEING_PULLED_BUT_WILL_SNAP_BACK
          );
        }

        @Override
        public void onRelease(boolean collapseEligible) {
          emitter.onNext(collapseEligible
              ? SubmissionPageState.BEING_PULLED_AND_ELIGIBLE_FOR_COLLAPSE
              : SubmissionPageState.BEING_PULLED_BUT_WILL_SNAP_BACK
          );
        }
      };
      SimpleExpandablePageStateChangeCallbacks stateChangeCallbacks = new SimpleExpandablePageStateChangeCallbacks() {
        @Override
        public void onPageExpanded() {
          emitter.onNext(SubmissionPageState.EXPANDED);
        }

        @Override
        public void onPageAboutToCollapse(long collapseAnimDuration) {
          emitter.onNext(SubmissionPageState.COLLAPSED);
        }
      };

      expandablePageLayout.addOnPullListener(pullListener);
      expandablePageLayout.addStateChangeCallbacks(stateChangeCallbacks);

      emitter.setCancellable(() -> {
        expandablePageLayout.removeOnPullListener(pullListener);
        expandablePageLayout.removeStateChangeCallbacks(stateChangeCallbacks);
      });

      // Initial callbacks.
      if (expandablePageLayout.isExpanded()) {
        stateChangeCallbacks.onPageExpanded();
      }
    });
  }

  private Observable<Boolean> streamContentVisibilityState(ScrollingRecyclerViewSheet commentListParentSheet) {
    return Observable.create(emitter -> {
      ScrollingRecyclerViewSheet.SheetScrollChangeListener scrollChangeListener = newScrollY -> {
        emitter.onNext(newScrollY > 0f);
      };
      commentListParentSheet.addOnSheetScrollChangeListener(scrollChangeListener);
      emitter.setCancellable(() -> commentListParentSheet.removeOnSheetScrollChangeListener(scrollChangeListener));

      // Initial callbacks.
      scrollChangeListener.onScrollChange(commentListParentSheet.currentScrollY());
    });
  }
}
