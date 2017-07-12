package me.saket.dank.ui.submission;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.CheckResult;
import android.support.v4.content.ContextCompat;

import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.data.StatusBarTint;
import me.saket.dank.utils.StatusBarTintProvider;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.PullToCollapseListener;
import me.saket.dank.widgets.InboxUI.SimpleExpandablePageStateChangeCallbacks;

class SubmissionStatusBarTintProvider {

  private final StatusBarTintProvider statusBarTintProvider;
  private final int defaultStatusBarColor;

  SubmissionStatusBarTintProvider(Context context) {
    int statusBarHeight = Views.statusBarHeight(context.getResources());
    defaultStatusBarColor = ContextCompat.getColor(context, R.color.color_primary_dark);
    statusBarTintProvider = new StatusBarTintProvider(statusBarHeight, defaultStatusBarColor);
  }

  @CheckResult
  public Observable<StatusBarTint> streamStatusBarTintColor(Observable<Bitmap> contentBitmapStream, ExpandablePageLayout expandablePageLayout) {
    Observable<Boolean> tintEligibleStream = createTintEligibleStream(expandablePageLayout).distinctUntilChanged();
    StatusBarTint defaultTint = StatusBarTint.create(defaultStatusBarColor, true /* isDark */);

    return contentBitmapStream
        .switchMapSingle(bitmap -> statusBarTintProvider.generateTint(bitmap))
        .distinctUntilChanged()
        .startWith(defaultTint)
        .flatMap(statusBarTint -> tintEligibleStream.map(tintEligible -> tintEligible ? statusBarTint : defaultTint));
  }

  private Observable<Boolean> createTintEligibleStream(ExpandablePageLayout expandablePageLayout) {
    Relay<Boolean> tintEligibleStream = PublishRelay.create();
    expandablePageLayout.addOnPullCallbacks(new PullToCollapseListener.OnPullListener() {
      @Override
      public void onPull(float deltaY, float currentTranslationY, boolean upwardPull, boolean deltaUpwardPull, boolean collapseEligible) {
        //Timber.i("collapseEligible: %s", collapseEligible);
        tintEligibleStream.accept(!collapseEligible);
      }

      @Override
      public void onRelease(boolean collapseEligible) {
        tintEligibleStream.accept(!collapseEligible);
      }
    });
    expandablePageLayout.addStateChangeCallbacks(new SimpleExpandablePageStateChangeCallbacks() {
      @Override
      public void onPageExpanded() {
        //Timber.i("Page expanded. Showing tint.");
        tintEligibleStream.accept(true);
      }

      @Override
      public void onPageAboutToCollapse(long collapseAnimDuration) {
        //Timber.i("Page about to collapse. Hiding tint.");
        tintEligibleStream.accept(false);
      }
    });
    return tintEligibleStream;
  }
}
