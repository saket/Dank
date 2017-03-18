package me.saket.dank.utils;

import android.app.Activity;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;
import android.webkit.WebView;
import android.widget.TextView;

import butterknife.ButterKnife;

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
                if (ViewCompat.isLaidOut(view)) {
                    onMeasureRunnable.run();

                    //noinspection deprecation
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
    }

    public interface OnStatusBarHeightCalculateListener{
        void onStatusBarHeightCalculated(int statusBarHeight);
    }

    public static void getStatusBarHeight(View rootViewGroup, OnStatusBarHeightCalculateListener listener) {
        rootViewGroup.setOnApplyWindowInsetsListener((v, insets) -> {
            listener.onStatusBarHeightCalculated(insets.getSystemWindowInsetTop());
            return insets;
        });
    }

    public static void getStatusBarHeight(Activity activity, OnStatusBarHeightCalculateListener listener) {
        getStatusBarHeight(ButterKnife.findById(activity, Window.ID_ANDROID_CONTENT), listener);
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

}
