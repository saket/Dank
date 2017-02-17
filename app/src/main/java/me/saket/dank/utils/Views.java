package me.saket.dank.utils;

import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.view.View;
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

    public static void setPaddingTop(View view, int paddingTop) {
        view.setPaddingRelative(view.getPaddingStart(), paddingTop, view.getPaddingEnd(), view.getPaddingBottom());
    }

    public static void setPaddingBottom(View view, int paddingBottom) {
        view.setPaddingRelative(view.getPaddingStart(), view.getPaddingTop(), view.getPaddingEnd(), paddingBottom);
    }

    @Nullable
    public static String previousUrlInHistory(WebView contentWebView) {
        WebBackForwardList history = contentWebView.copyBackForwardList();
        if (history.getSize() > 1) {
            WebHistoryItem previousItem = history.getItemAtIndex(history.getCurrentIndex() - 1);
            return previousItem.getUrl();
        }
        return null;
    }

}
