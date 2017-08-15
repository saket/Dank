package me.saket.dank.ui.user;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.transition.Transition;
import android.transition.Transition.EpicenterCallback;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import me.saket.dank.R;
import timber.log.Timber;

/**
 * Mimics {@link PopupMenu}'s API 23+ entry animation and enables dismiss-on-outside-touch.
 */
public abstract class PopupWindowWithMaterialTransition extends PopupWindow {

  private final WindowManager windowManager;
  private int gravity;
  private Point showLocation;

  public PopupWindowWithMaterialTransition(Context context) {
    super(context, null, 0, R.style.DankPopupWindow);
    windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
  }

  public void showWithAnchor(View anchorView, int gravity) {
    int[] anchorViewLoc = new int[2];
    anchorView.getLocationOnScreen(anchorViewLoc);
    showAtLocation(anchorView, gravity, new Point(anchorViewLoc[0], anchorViewLoc[1]));
  }

  public void showAtLocation(View anchorView, int gravity, Point showLocation) {
    this.gravity = gravity;
    this.showLocation = showLocation;

    if (getContentView() == null) {
      throw new IllegalStateException("setContentView was not called with a view to display.");
    }

    // PopupWindow has a thin border around the content. This removes it.
    setBackgroundDrawable(null);

    // Dismiss on outside touch.
    setFocusable(true);
    setOutsideTouchable(true);

    boolean isTopGravity = (gravity | Gravity.TOP) == gravity;
    Point positionToShow = calculatePositionWithAnchorWithoutGoingOutsideWindow(showLocation, getContentView(), isTopGravity);
    showAtLocation(anchorView, Gravity.TOP | Gravity.START, positionToShow.x, positionToShow.y);

    addBackgroundDimming();
    playPopupEnterTransition(showLocation);

    getContentView().getViewTreeObserver().addOnGlobalLayoutListener(updatePopupPositionOnContentSizeChangeListener);
  }

  @Override
  public void dismiss() {
    getContentView().getViewTreeObserver().removeOnGlobalLayoutListener(updatePopupPositionOnContentSizeChangeListener);
    super.dismiss();
  }

  private OnGlobalLayoutListener updatePopupPositionOnContentSizeChangeListener = new OnGlobalLayoutListener() {
    private Size contentSize = new Size(0, 0);

    @Override
    public void onGlobalLayout() {
      Timber.i("Global layout");

      if (getContentView().getWidth() != contentSize.getWidth() || getContentView().getHeight() != contentSize.getHeight()) {
        contentSize = new Size(getContentView().getWidth(), getContentView().getHeight());
        Timber.i("Content size changed: %s", contentSize);

        // This will also run on the 1st time, but PopupWindow will ignore it when
        updatePopupLocationWithNewDimensions();
      }
    }
  };

  public void updatePopupLocationWithNewDimensions() {
    boolean isTopGravity = (gravity | Gravity.TOP) == gravity;
    Point positionToShow = calculatePositionWithAnchorWithoutGoingOutsideWindow(showLocation, getContentView(), isTopGravity);
    update(positionToShow.x, positionToShow.y, -1, -1);
  }

  private void addBackgroundDimming() {
    View decorView = getContentView().getRootView();
    WindowManager.LayoutParams params = (WindowManager.LayoutParams) decorView.getLayoutParams();
    params.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
    params.dimAmount = 0.3f;
    windowManager.updateViewLayout(decorView, params);
  }

  private void playPopupEnterTransition(Point showLocation) {
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

        // Note: EpicenterTranslateClipReveal uses the epicenter's center location for animation.
        Rect epicenter = new Rect(showLocation.x, showLocation.y, showLocation.x, showLocation.y);
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

  public Point calculatePositionWithAnchorWithoutGoingOutsideWindow(Point showLocation, View contentView, boolean isTopGravity) {
    contentView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    int contentWidth = contentView.getMeasuredWidth();
    int contentHeight = contentView.getMeasuredHeight();

    Point displaySize = new Point();
    windowManager.getDefaultDisplay().getSize(displaySize);
    int screenWidth = displaySize.x;
    int screenHeight = displaySize.y;

    int xPos = showLocation.x;
    int yPos = showLocation.y;

    // Display above the anchor view.
    if (isTopGravity || yPos + contentHeight > screenHeight) {
      yPos = showLocation.y - contentHeight;
    }

    // Keep the right edge of the popup on the screen.
    if (xPos + contentWidth > screenWidth) {
      xPos = showLocation.x - ((showLocation.x + contentWidth) - screenWidth);
    }
    return new Point(xPos, yPos);
  }
}
