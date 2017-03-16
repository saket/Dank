package me.saket.dank.ui;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;

import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

/**
 * An Activity that can be dismissed by pulling it vertically.
 */
public abstract class DankPullCollapsibleActivity extends DankActivity {

    private IndependentExpandablePageLayout activityPageLayout;
    private int activityParentToolbarHeight;
    private Rect expandedFromRect;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
    }

    protected void setupActivityExpandablePage(IndependentExpandablePageLayout pageLayout, int parentToolbarHeight) {
        activityPageLayout = pageLayout;
        activityParentToolbarHeight = parentToolbarHeight;

        activityPageLayout.setPullToCollapseDistanceThreshold(parentToolbarHeight);

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
    }

    protected void expandFromBelowToolbar() {
        Rect toolbarRect = new Rect(0, activityParentToolbarHeight, activityPageLayout.getWidth(), 0);
        expandFrom(toolbarRect);
    }

    protected void expandFrom(Rect fromRect) {
        expandedFromRect = fromRect;
        activityPageLayout.expandFrom(fromRect);
    }

    @Override
    public void finish() {
        activityPageLayout.collapseTo(expandedFromRect);
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
