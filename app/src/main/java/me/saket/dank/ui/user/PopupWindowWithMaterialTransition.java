package me.saket.dank.ui.user;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.transition.Transition;
import android.transition.Transition.EpicenterCallback;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.PopupMenu;
import android.widget.PopupWindow;

import me.saket.dank.R;
import me.saket.dank.utils.Views;

/**
 * Mimics {@link PopupMenu}'s API 23+ entry animation and enables dismiss-on-outside-touch.
 * Abstract because why not.
 */
public abstract class PopupWindowWithMaterialTransition extends PopupWindow {

  private final WindowManager windowManager;

  public PopupWindowWithMaterialTransition(Context context) {
    super(context, null, 0, R.style.DankPopupWindow);
    this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
  }

  @Override
  public void setContentView(View contentView) {
    super.setContentView(contentView);
  }

  public void showWithAnchor(View anchorView, int gravity) {
    int[] anchorViewLoc = new int[2];
    anchorView.getLocationOnScreen(anchorViewLoc);
    showAtLocation(anchorView, gravity, new Point(anchorViewLoc[0], anchorViewLoc[1]));
  }

  public void showAtLocation(View anchorView, int gravity, Point showLocation) {
    if (getContentView() == null) {
      throw new IllegalStateException("setContentView was not called with a view to display.");
    }

    // PopupWindow has a thin border around the content. This removes it.
    setBackgroundDrawable(null);

    // Dismiss on outside touch.
    setFocusable(true);
    setOutsideTouchable(true);

    boolean isTopGravity = (gravity | Gravity.TOP) == gravity;
    Point positionToShow = adjustPositionWithAnchorWithoutGoingOutsideWindow(showLocation, getContentView(), isTopGravity);
    showAtLocation(anchorView, Gravity.TOP | Gravity.START, positionToShow.x, positionToShow.y);

    addBackgroundDimming();
    playPopupEnterTransition(positionToShow, anchorView);
  }

  @Override
  public void showAsDropDown(View anchor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void showAsDropDown(View anchor, int xoff, int yoff) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
    throw new UnsupportedOperationException();
  }

  private void addBackgroundDimming() {
    View decorView = getContentView().getRootView();
    WindowManager.LayoutParams params = (WindowManager.LayoutParams) decorView.getLayoutParams();
    params.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
    params.dimAmount = 0.3f;
    windowManager.updateViewLayout(decorView, params);
  }

  private void playPopupEnterTransition(Point showLocation, View anchorView) {
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
        Rect epicenter = calculateTransitionEpicenter(anchorView, popupDecorView, showLocation);
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

  protected abstract Rect calculateTransitionEpicenter(View anchor, ViewGroup popupDecorView, Point showLocation);

  protected Point adjustPositionWithAnchorWithoutGoingOutsideWindow(Point showLocation, View contentView, boolean isTopGravity) {
    if (contentView.getLayoutParams() != null) {
      contentView.measure(
          MeasureSpec.makeMeasureSpec(contentView.getLayoutParams().width, MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

    } else {
      contentView.measure(
          MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
          MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    }
    int contentWidth = contentView.getMeasuredWidth();
    int contentHeight = contentView.getMeasuredHeight();

    Point displaySize = new Point();
    windowManager.getDefaultDisplay().getSize(displaySize);
    int screenWidth = displaySize.x;
    int screenHeight = displaySize.y;

    int statusBarHeight = Views.statusBarHeight(contentView.getResources());
    int heightAvailableAboveShowPosition = showLocation.y - statusBarHeight;

    int xPos = showLocation.x;
    int yPos = showLocation.y;

    // Show the popup below the content if there's not enough room available above.
    if (contentHeight > heightAvailableAboveShowPosition) {
      yPos = showLocation.y;

    } else {
      // Display above the anchor view.
      if (isTopGravity || yPos + contentHeight > screenHeight) {
        yPos = showLocation.y - contentHeight;
      }
    }

    // Keep the right edge of the popup on the screen.
    if (xPos + contentWidth > screenWidth) {
      xPos = showLocation.x - ((showLocation.x + contentWidth) - screenWidth);
    }
    return new Point(xPos, yPos);
  }
}
