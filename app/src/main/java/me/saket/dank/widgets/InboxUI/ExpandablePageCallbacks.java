package me.saket.dank.widgets.InboxUI;

/**
 * Implement this to receive callbacks about an <code>ExpandingPage</code>. Use with
 * {@link ExpandablePageLayout#addCallbacks(ExpandablePageCallbacks...)}. If you use a Fragment
 * inside your <code>ExpandablePage</code>, it's best to make your Fragment implement this interface.
 */
public interface ExpandablePageCallbacks {

    /**
     * Called when the user has selected an item and the <code>ExpandablePage</code> is going to be expand.
     */
    void onPageAboutToExpand(long expandAnimDuration);

    /**
     * Called when either the <code>ExpandablePage</code>'s expand animation is complete or if the
     * <code>ExpandablePage</code> was expanded immediately. At this time, the page is fully covering the list.
     */
    void onPageExpanded();

    /**
     * Called when the user has chosen to close the expanded item and the <code>ExpandablePage</code> is going to be collapse.
     */
    void onPageAboutToCollapse(long collapseAnimDuration);

    /**
     * Called when the page's collapse animation is complete. At this time, it's totally invisible to the user.
     */
    void onPageCollapsed();

}
