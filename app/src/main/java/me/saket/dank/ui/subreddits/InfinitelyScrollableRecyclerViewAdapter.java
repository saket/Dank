package me.saket.dank.ui.subreddits;

public interface InfinitelyScrollableRecyclerViewAdapter {

  /**
   * Decorators == headers, footers, etc.
   */
  int getItemCountMinusDecorators();
}
