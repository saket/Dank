package me.saket.dank.ui.user;

import android.content.Context;
import android.graphics.Rect;
import android.transition.Transition;
import android.transition.Transition.EpicenterCallback;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import me.saket.dank.R;

/**
 * Mimics {@link PopupMenu}'s API 23+ entry animation and enables dismiss-on-outside-touch.
 */
public abstract class PopupWindowWithTransition extends PopupWindow {

  private final WindowManager windowManager;

  public PopupWindowWithTransition(Context context) {
    super(context, null, 0, R.style.DankPopupWindow);
    windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
  }

  /**
   * In case there is stuff to do right before displaying.
   */
  protected void onShow() {
  }

  public void show(View anchorView) {
    if (getContentView() == null) {
      throw new IllegalStateException("setContentView was not called with a view to display.");
    }

    // PopupWindow has a thin border around the content. This removes it.
    setBackgroundDrawable(null);

    // Dismiss on outside touch.
    setFocusable(true);
    setOutsideTouchable(true);

    // Callback for subclasses.
    onShow();

    showAsDropDown(anchorView);

    addBackgroundDimming();
    playPopupEnterTransition(anchorView);
  }

  private void addBackgroundDimming() {
    View decorView = getContentView().getRootView();
    WindowManager.LayoutParams params = (WindowManager.LayoutParams) decorView.getLayoutParams();
    params.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
    params.dimAmount = 0.3f;
    windowManager.updateViewLayout(decorView, params);
  }

  private void playPopupEnterTransition(View anchorView) {
    ViewGroup popupDecorView = (ViewGroup) getContentView().getRootView();
    final Transition enterTransition = TransitionInflater.from(popupDecorView.getContext()).inflateTransition(R.transition.popupwindow_enter);

    // Postpone the enter transition after the first layout pass.
    final ViewTreeObserver observer = popupDecorView.getViewTreeObserver();
    observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        final ViewTreeObserver observer = popupDecorView.getViewTreeObserver();
        if (observer != null) {
          observer.removeOnGlobalLayoutListener(this);
        }

        final Rect epicenter = calculateEpicenterBounds(anchorView);
        enterTransition.setEpicenterCallback(new EpicenterCallback() {
          @Override
          public Rect onGetEpicenter(Transition transition) {
            return epicenter;
          }
        });

        final int count = popupDecorView.getChildCount();
        for (int i = 0; i < count; i++) {
          final View child = popupDecorView.getChildAt(i);
          enterTransition.addTarget(child);
          child.setVisibility(View.INVISIBLE);
        }

        TransitionManager.beginDelayedTransition(popupDecorView, enterTransition);

        for (int i = 0; i < count; i++) {
          final View child = popupDecorView.getChildAt(i);
          child.setVisibility(View.VISIBLE);
        }
      }
    });

  }

  private Rect calculateEpicenterBounds(View anchorView) {
    int[] anchorLocation = new int[2];
    anchorView.getLocationOnScreen(anchorLocation);

    int[] popupLocation = new int[2];
    View decorView = getContentView().getRootView();
    decorView.getLocationOnScreen(popupLocation);


    // Compute the position of the anchor relative to the popup.
    final Rect bounds = new Rect(0, 0, anchorView.getWidth(), anchorView.getHeight());
    bounds.offset(anchorLocation[0] - popupLocation[0], anchorLocation[1] - popupLocation[1]);
    return bounds;
  }
}
