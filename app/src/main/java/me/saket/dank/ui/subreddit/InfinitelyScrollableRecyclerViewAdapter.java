package me.saket.dank.ui.subreddit;

public interface InfinitelyScrollableRecyclerViewAdapter {

  /**
   * Decorators == headers, footers, etc.
   */
  int getItemCountMinusDecorators();
}
