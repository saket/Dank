package me.saket.dank.ui;

import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

/**
 * An Activity that can be dismissed by pulling it vertically.
 *
 * Checklist for subclasses:
 * 1. Call {@link #setPullToCollapseEnabled(boolean)} if needed before super.onCreate().
 * 2. Call {@link #setupContentExpandablePage(IndependentExpandablePageLayout)}.
 * 3. Finally, call {@link #expandFromBelowToolbar()} or {@link #expandFrom(Rect)}.
 */
public abstract class DankPullCollapsibleActivity extends DankActivity {

    private IndependentExpandablePageLayout activityPageLayout;
    private Rect expandedFromRect;
    private int activityParentToolbarHeight;
    private boolean pullCollapsibleEnabled;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (pullCollapsibleEnabled) {
            overridePendingTransition(0, 0);
        }

        TypedArray typedArray = obtainStyledAttributes(new int[] { android.R.attr.actionBarSize });
        activityParentToolbarHeight = typedArray.getDimensionPixelSize(0, 0);
        typedArray.recycle();
    }

    /**
     * Defaults to true. When disabled, this behaves like a normal Activity with no expandable page animations.
     * Should be called before onCreate().
     */
    protected void setPullToCollapseEnabled(boolean enabled) {
        pullCollapsibleEnabled = enabled;
    }

    protected void setupContentExpandablePage(IndependentExpandablePageLayout pageLayout) {
        activityPageLayout = pageLayout;

        if (pullCollapsibleEnabled) {
            activityPageLayout.setPullToCollapseDistanceThreshold(activityParentToolbarHeight);
            pageLayout.setCallbacks(new IndependentExpandablePageLayout.Callbacks() {
                @Override
                public void onPageFullyCollapsed() {
                    DankPullCollapsibleActivity.super.finish();
                    overridePendingTransition(0, 0);
                }

                @Override
                public void onPageRelease(boolean collapseEligible) {
                    if (collapseEligible) {
                        finish();
                    }
                }
            });

        } else {
            activityPageLayout.setPullToCollapseEnabled(false);
            activityPageLayout.expandImmediately();
        }
    }

    protected void expandFromBelowToolbar() {
        Views.executeOnMeasure(activityPageLayout, () -> {
            Rect toolbarRect = new Rect(0, activityParentToolbarHeight, activityPageLayout.getWidth(), 0);
            expandFrom(toolbarRect);
        });
    }

    protected void expandFrom(Rect fromRect) {
        expandedFromRect = fromRect;
        activityPageLayout.expandFrom(fromRect);
    }

    @Override
    public void finish() {
        if (pullCollapsibleEnabled) {
            activityPageLayout.collapseTo(expandedFromRect);
        } else {
            super.finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;

        } else {
            return super.onOptionsItemSelected(item);
        }
    }

}
