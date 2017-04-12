package me.saket.dank.widgets.InboxUI;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;

/**
 * This can live without an {@link InboxRecyclerView}.
 */
public class IndependentExpandablePageLayout extends ExpandablePageLayout {

    public static final long ANIMATION_DURATION = 300;

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
        setAnimationDuration(ANIMATION_DURATION);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (isInEditMode()) {
            expandImmediately();
            setClippedDimensions(r, b);
        }
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
     *
     * @param fromShapeRect Initial dimensions of this page.
     */
    public void expandFrom(Rect fromShapeRect) {
        setClippedDimensions(getWidth(), 0);
        expand(new InboxRecyclerView.ExpandInfo(-1, -1, fromShapeRect));
    }

    /**
     * @param toShapeRect Final dimensions of this page, when it fully collapses.
     */
    public void collapseTo(Rect toShapeRect) {
        collapse(new InboxRecyclerView.ExpandInfo(-1, -1, toShapeRect));
    }

}
