package me.saket.dank.utils;

import android.content.res.Resources;
import android.graphics.Rect;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;
import android.webkit.WebView;
import android.widget.TextView;

/**
 * Utility methods for Views.
 */
public class Views {

    /**
     * Execute a runnable when the next global layout happens for a <code>View</code>. Example usage includes
     * waiting for a list to draw its children just after you have updated its adapter's data-set.
     */
    public static void executeOnNextLayout(final View view, Runnable runnable) {
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                runnable.run();

                //noinspection deprecation
                view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
    }

    /**
     * Execute a runnable when a <var>view</var>'s dimensions get measured and is laid out on the screen.
     */
    public static void executeOnMeasure(View view, Runnable onMeasureRunnable) {
        if (view.isInEditMode() || view.isLaidOut()) {
            onMeasureRunnable.run();
            return;
        }

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (view.isLaidOut()) {
                    onMeasureRunnable.run();

                    //noinspection deprecation
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
    }

    public static int statusBarHeight(Resources resources) {
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public static void setPaddingStart(View view, int paddingStart) {
        view.setPaddingRelative(paddingStart, view.getPaddingTop(), view.getPaddingEnd(), view.getPaddingBottom());
    }

    public static void setPaddingTop(View view, int paddingTop) {
        view.setPaddingRelative(view.getPaddingStart(), paddingTop, view.getPaddingEnd(), view.getPaddingBottom());
    }

    public static void setHeight(View view, int height) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = height;
        view.setLayoutParams(params);
    }

    /**
     * Get the previous page's URL in <var>webView</var>'s history.
     */
    @Nullable
    public static String previousUrlInHistory(WebView webView) {
        WebBackForwardList history = webView.copyBackForwardList();
        if (history.getSize() > 1) {
            WebHistoryItem previousItem = history.getItemAtIndex(history.getCurrentIndex() - 1);
            return previousItem.getUrl();
        }
        return null;
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

    public static void setCompoundDrawableEnd(TextView textView, @DrawableRes int iconResId) {
        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                textView.getCompoundDrawables()[0],
                textView.getCompoundDrawables()[1],
                iconResId != 0 ? textView.getContext().getDrawable(iconResId) : null,
                textView.getCompoundDrawables()[3]
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

}
