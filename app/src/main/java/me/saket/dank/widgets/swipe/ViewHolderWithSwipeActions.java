package me.saket.dank.widgets.swipe;

/**
 * Implemented by ViewHolders that wrap a {@link SwipeableLayout} for RecyclerView rows.
 */
public interface ViewHolderWithSwipeActions {
  SwipeableLayout getSwipeableLayout();
}
