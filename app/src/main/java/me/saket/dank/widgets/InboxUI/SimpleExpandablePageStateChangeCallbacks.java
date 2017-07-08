package me.saket.dank.widgets.InboxUI;

/**
 * Empty implementations of {@link ExpandablePageLayout.StateChangeCallbacks}. This waiy, any custom listener that cares only about a subset of the
 * methods of this listener can simply subclass this adapter class instead of implementing the interface directly.
 */
public abstract class SimpleExpandablePageStateChangeCallbacks implements ExpandablePageLayout.StateChangeCallbacks {

  @Override
  public void onPageAboutToExpand(long expandAnimDuration) {

  }

  @Override
  public void onPageExpanded() {

  }

  @Override
  public void onPageAboutToCollapse(long collapseAnimDuration) {

  }

  @Override
  public void onPageCollapsed() {

  }

}
