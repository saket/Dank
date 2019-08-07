package me.saket.dank.utils;

import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.CheckResult;
import android.support.annotation.DrawableRes;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.TextView;

import io.reactivex.Completable;
import timber.log.Timber;

import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;

/**
 * Utility methods for Views.
 * <p>
 * Convert all "executeOn*" methods to Rx.
 */
public class Views {

  private static int statusBarHeight = -1;

  /**
   * Execute a runnable when the next global layout happens for a <code>View</code>. Example usage includes
   * waiting for a list to draw its children just after you have updated its adapter's data-set.
   */
  public static void executeOnNextLayout(final View view, Runnable runnable) {
    view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        //noinspection deprecation
        view.getViewTreeObserver().removeGlobalOnLayoutListener(this);

        runnable.run();
      }
    });
  }

  /**
   * Execute a runnable when a <var>view</var>'s dimensions get measured and is laid out on the screen.
   */
  public static void executeOnMeasure(View view, Runnable onMeasureRunnable) {
    executeOnMeasure(view, false, onMeasureRunnable);
  }

  /**
   * Execute a runnable when a <var>view</var>'s dimensions get measured and is laid out on the screen.
   *
   * @param consumeOnPreDraw When true, the pre-draw event will be consumed so that it never reaches the
   *                         View. This way, the View will not be notified of its size until the next
   *                         draw pass.
   */
  public static void executeOnMeasure(View view, boolean consumeOnPreDraw, Runnable onMeasureRunnable) {
    if (view.isInEditMode() || view.isLaidOut()) {
      onMeasureRunnable.run();
      return;
    }

    view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
      @Override
      public boolean onPreDraw() {
        if (view.isLaidOut()) {
          //noinspection deprecation
          view.getViewTreeObserver().removeOnPreDrawListener(this);

          onMeasureRunnable.run();

          if (consumeOnPreDraw) {
            return false;
          }

        } else if (view.getVisibility() == View.GONE) {
          Timber.w("View's visibility is set to Gone. It'll never be measured: %s", view);
          view.getViewTreeObserver().removeOnPreDrawListener(this);
        }

        return true;
      }
    });
  }

  /**
   * Wait until a <var>view</var>'s dimensions get measured and is laid out on the screen.
   */
  @CheckResult
  public static Completable rxWaitTillMeasured(View view) {
    return rxWaitTillMeasured(view, false);
  }

  /**
   * Wait until a <var>view</var>'s dimensions get measured and is laid out on the screen.
   *
   * @param consumeOnPreDraw When true, the pre-draw event will be consumed so that it never reaches the
   *                         View. This way, the View will not be notified of its size until the next
   *                         draw pass.
   */
  @CheckResult
  public static Completable rxWaitTillMeasured(View view, boolean consumeOnPreDraw) {
    return Completable.create(emitter -> {
      if (view.isInEditMode() || view.isLaidOut()) {
        emitter.onComplete();
        return;
      }

      ViewTreeObserver.OnPreDrawListener listener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
          if (view.isLaidOut()) {
            //noinspection deprecation
            view.getViewTreeObserver().removeOnPreDrawListener(this);

            emitter.onComplete();

            if (consumeOnPreDraw) {
              return false;
            }

          } else if (view.getVisibility() == View.GONE) {
            Timber.w("View's visibility is set to Gone. It'll never be measured: %s", view);
            view.getViewTreeObserver().removeOnPreDrawListener(this);
          }

          return true;
        }
      };

      view.getViewTreeObserver().addOnPreDrawListener(listener);
      emitter.setCancellable(() -> {
        view.getViewTreeObserver().removeOnPreDrawListener(listener);
      });
    });
  }

  public static int statusBarHeight(Resources resources) {
    if (statusBarHeight == -1) {
      int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
      if (resourceId > 0) {
        statusBarHeight = resources.getDimensionPixelSize(resourceId);
      } else {
        statusBarHeight = 0;
      }
    }
    return statusBarHeight;
  }

  public static void setPaddingTop(View view, int paddingTop) {
    view.setPaddingRelative(view.getPaddingStart(), paddingTop, view.getPaddingEnd(), view.getPaddingBottom());
  }

  public static void setPaddingBottom(View view, int paddingBottom) {
    view.setPaddingRelative(view.getPaddingStart(), view.getPaddingTop(), view.getPaddingEnd(), paddingBottom);
  }

  public static void setPaddingRight(View view, int paddingRight) {
    view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), paddingRight, view.getPaddingBottom());
  }

  public static int getPaddingVertical(View view) {
    return view.getPaddingTop() + view.getPaddingBottom();
  }

  public static void setPaddingVertical(View view, int padding) {
    view.setPaddingRelative(view.getPaddingStart(), padding, view.getPaddingEnd(), padding);
  }

  public static void setPaddingVertical(View view, int paddingTop, int paddingBottom) {
    view.setPaddingRelative(view.getPaddingStart(), paddingTop, view.getPaddingEnd(), paddingBottom);
  }

  public static void setDimensions(View view, int width, int height) {
    ViewGroup.LayoutParams params = view.getLayoutParams();
    params.width = width;
    params.height = height;
    view.setLayoutParams(params);
  }

  public static void setWidth(View view, int width) {
    ViewGroup.LayoutParams params = view.getLayoutParams();
    params.width = width;
    view.setLayoutParams(params);
  }

  public static void setHeight(View view, int height) {
    ViewGroup.LayoutParams params = view.getLayoutParams();
    params.height = height;
    view.setLayoutParams(params);
  }

  public static void setMarginStart(View view, int marginStart) {
    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
    params.leftMargin = marginStart;
    view.setLayoutParams(params);
  }

  public static void setMarginTop(View view, int marginTop) {
    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
    params.topMargin = marginTop;
    view.setLayoutParams(params);
  }

  public static void setMarginBottom(View view, int marginBottom) {
    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
    params.bottomMargin = marginBottom;
    view.setLayoutParams(params);
  }

  public static void setCompoundDrawableStart(TextView textView, @DrawableRes int iconResId) {
    textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
        iconResId != 0 ? textView.getContext().getDrawable(iconResId) : null,
        textView.getCompoundDrawablesRelative()[1],
        textView.getCompoundDrawablesRelative()[2],
        textView.getCompoundDrawablesRelative()[3]
    );
  }

  public static void setCompoundDrawableEnd(TextView textView, @DrawableRes int iconResId) {
    textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
        textView.getCompoundDrawablesRelative()[0],
        textView.getCompoundDrawablesRelative()[1],
        iconResId != 0 ? textView.getContext().getDrawable(iconResId) : null,
        textView.getCompoundDrawablesRelative()[3]
    );
  }

  /**
   * Check whether a touch event's points lie on a View. This does not consider if it's overlapped or not.
   */
  public static boolean touchLiesOn(View view, float x, float y) {
    return globalVisibleRect(view).contains((int) x, (int) y);
  }

  public static Rect globalVisibleRect(View view) {
    Rect rect = new Rect();
    view.getGlobalVisibleRect(rect);
    return rect;
  }

  public static Point locationOnScreen(View view) {
    int[] location = new int[2];
    view.getLocationOnScreen(location);
    return new Point(location[0], location[1]);
  }

  public static int getTopRelativeToParent(ViewGroup parent, View view) {
    int top = 0;
    View nextViewToCheck = view;

    while (true) {
      top += nextViewToCheck.getTop();
      if (parent == nextViewToCheck.getParent()) {
        break;
      }
      nextViewToCheck = ((ViewGroup) nextViewToCheck.getParent());
    }

    return top;
  }

  public static void setTextWithCursor(EditText editText, CharSequence string) {
    editText.setText(string);
    editText.setSelection(string.length());
  }

  public static ExpandablePageLayout.OnPullToCollapseIntercepter verticalScrollPullToCollapseIntercepter(View view) {
    return (event, downX, downY, upwardPagePull) -> {
      boolean touchLiesOnList = Views.touchLiesOn(view, downX, downY);
      int directionInt = upwardPagePull ? 1 : -1;
      boolean canListScrollFurther = view.canScrollVertically(directionInt);
      return touchLiesOnList && canListScrollFurther;
    };
  }
}
