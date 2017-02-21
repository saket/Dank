package me.saket.dank.utils;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;
import android.webkit.WebView;

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
        if (ViewCompat.isLaidOut(view)) {
            onMeasureRunnable.run();
            return;
        }

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (ViewCompat.isLaidOut(view)) {
                    onMeasureRunnable.run();

                    //noinspection deprecation
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
    }

    public static void setPaddingStart(View view, int paddingStart) {
        view.setPaddingRelative(paddingStart, view.getPaddingTop(), view.getPaddingEnd(), view.getPaddingBottom());
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

    public static void setMarginTop(View view, int marginTop) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        params.topMargin = marginTop;
        view.setLayoutParams(params);
    }

    public static GestureDetector onSingleTapDetector(Context context, Runnable runnable) {
        return new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                runnable.run();
                return false;
            }
        });
    }

}
