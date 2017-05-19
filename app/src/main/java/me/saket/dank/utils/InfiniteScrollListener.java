package me.saket.dank.utils;

import android.support.annotation.CheckResult;
import android.support.annotation.FloatRange;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.jakewharton.rxbinding2.support.v7.widget.RecyclerViewScrollEvent;
import com.jakewharton.rxbinding2.support.v7.widget.RxRecyclerView;

import io.reactivex.Observable;
import io.reactivex.functions.Predicate;

/**
 * Listens for scroll events and dispatches
 */
public class InfiniteScrollListener {

  private final float loadThresholdFactor;
  private final RecyclerView recyclerView;
  private boolean isLoadOngoing;
  private boolean emitInitialEvent;

  /**
   * @param loadThresholdFactor %age of items after which new items should be loaded. For eg., with 20 items
   *                            and a factor of 0.6f, a new load will be triggered when the list reaches the
   *                            12th item.
   */
  public static InfiniteScrollListener create(RecyclerView recyclerView, @FloatRange(from = 0f, to = 1f) float loadThresholdFactor) {
    return new InfiniteScrollListener(recyclerView, loadThresholdFactor);
  }

  private InfiniteScrollListener(RecyclerView recyclerView, @FloatRange(from = 0f, to = 1f) float loadThresholdFactor) {
    this.loadThresholdFactor = loadThresholdFactor;
    this.recyclerView = recyclerView;
  }

  public void setLoadOngoing(boolean loadOngoing) {
    isLoadOngoing = loadOngoing;
  }

  public InfiniteScrollListener setEmitInitialEvent(boolean emitInitial) {
    this.emitInitialEvent = emitInitial;
    return this;
  }

  @CheckResult
  public Observable<?> emitWhenLoadNeeded() {
    // RxRecyclerView doesn't emit an initial value, unlike all other bindings. So do it manually.
    RecyclerViewScrollEvent initialScrollEvent = RecyclerViewScrollEvent.create(recyclerView, 0, 0);

    Observable<RecyclerViewScrollEvent> scrollEventsStream = RxRecyclerView.scrollEvents(recyclerView);
    if (emitInitialEvent) {
      scrollEventsStream = scrollEventsStream.startWith(initialScrollEvent);
    }

    return scrollEventsStream
        .filter(event -> event.dy() > 0)  // This ensures the list has enough items to be scrollable.
        .filter(o -> !isLoadOngoing)
        .filter(shouldLoadMore());
  }

  private Predicate<RecyclerViewScrollEvent> shouldLoadMore() {
    return scrollEvent -> {
      RecyclerView recyclerView = scrollEvent.view();
      int totalItemCount = recyclerView.getAdapter().getItemCount();
      int lastVisibleItemPosition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();

      float threshold = (float) totalItemCount * loadThresholdFactor;
      return lastVisibleItemPosition + 1 >= threshold;
    };
  }

}
