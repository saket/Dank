package me.saket.dank.widgets;

import android.app.Activity;
import android.support.annotation.CheckResult;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import io.reactivex.Observable;

public class KeyboardVisibilityDetector {

  private final View activityContentLayout;
  private final ViewGroup contentLayout;
  private final int statusBarHeight;

  public KeyboardVisibilityDetector(Activity activity, ViewGroup contentLayout, int statusBarHeight) {
    this.activityContentLayout = getDecorViewChild(activity);
    this.contentLayout = contentLayout;
    this.statusBarHeight = statusBarHeight;
  }

  @CheckResult
  public Observable<Boolean> streamKeyboardVisibilityChanges() {
    return Observable.create(emitter -> {
      ViewTreeObserver.OnGlobalLayoutListener layoutListener = () -> {
        boolean isKeyboardVisible = contentLayout.getHeight() < activityContentLayout.getHeight() - statusBarHeight;
        emitter.onNext(isKeyboardVisible);
      };
      contentLayout.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
      emitter.setCancellable(() -> contentLayout.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener));

      // Initial value.
      contentLayout.post(() -> layoutListener.onGlobalLayout());
    });
  }

  /**
   * Finds the first grand-child of Window's decor-View, which does not get resized when the keyboard is shown and does
   * not contain space for any System Ui bars either.
   * <p>
   * DecorView <- does not get resized and contains space for system Ui bars.
   * - LinearLayout <- does not get resized and contains space for only status bar.
   * -- Activity content <- Gets resized.
   */
  private View getDecorViewChild(Activity activity) {
    return ((ViewGroup) activity.getWindow().getDecorView()).getChildAt(0);
  }
}
