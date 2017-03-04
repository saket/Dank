package me.saket.dank.widgets.InboxUI;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;

/**
 * This can live without an {@link InboxRecyclerView}.
 */
public class IndependentExpandablePageLayout extends ExpandablePageLayout {

    private int parentToolbarHeight;

    public interface Callbacks {
        /**
         * Called when this page has fully collapsed and is no longer visible.
         */
        void onPageFullyCollapsed();

        /**
         * Called when this page was released while being pulled.
         *
         * @param collapseEligible Whether the page was pulled enough for collapsing it.
         */
        void onPageRelease(boolean collapseEligible);
    }

    public IndependentExpandablePageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setCollapsedAlpha(1f);
    }

    /**
     * Used for two things: calculating the pull-to-collapse distance threshold and the
     * location where this page collapses to (so that it appears to collapse below the
     * parent Activity's toolbar instead of below the status bar).
     */
    public void setParentActivityToolbarHeight(int toolbarHeight) {
        parentToolbarHeight = toolbarHeight;
        setPullToCollapseDistanceThreshold(toolbarHeight);
    }

    public void setCallbacks(Callbacks callbacks) {
        super.setInternalCallbacksList(new InternalPageCallbacks() {
            @Override
            public void onPageFullyCovered() {

            }

            @Override
            public void onPageAboutToCollapse() {

            }

            @Override
            public void onPageFullyCollapsed() {
                callbacks.onPageFullyCollapsed();
            }

            @Override
            public void onPagePull(float deltaY) {

            }

            @Override
            public void onPageRelease(boolean collapseEligible) {
                callbacks.onPageRelease(collapseEligible);
            }
        });
    }

    /**
     * Expands this page (with animation) so that it fills the whole screen.
     */
    public void expandFromBelowToolbar() {
        setClippedDimensions(getWidth(), 0);
        expand(new InboxRecyclerView.ExpandInfo(-1, -1, new Rect(0, parentToolbarHeight, getWidth(), 0)));
    }

    public void collapseBelowToolbar() {
        collapse(new InboxRecyclerView.ExpandInfo(-1, -1, new Rect(0, parentToolbarHeight, getWidth(), 0)));
    }

}
