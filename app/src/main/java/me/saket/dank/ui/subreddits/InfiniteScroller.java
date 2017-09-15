package me.saket.dank.ui.subreddits;


import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import io.reactivex.Observable;

/**
 * @author https://github.com/matzuk/PaginationSample
 */
public class InfiniteScroller {

  // For first start of items loading then on RecyclerView there are not items and no scrolling.
  private static final int EMPTY_LIST_ITEMS_COUNT = 0;

  // Fetch more items once the list has scrolled past 75% of its items.
  public static final float DEFAULT_LOAD_THRESHOLD = 0.65f;

  private RecyclerView recyclerView;
  private int emptyListCount;
  private float scrollThreshold;

  public static Builder buildPagingObservable(RecyclerView recyclerView) {
    return new Builder(recyclerView);
  }

  public static Observable<Object> streamPagingRequests(RecyclerView recyclerView) {
    return new Builder(recyclerView).build().streamPagingRequest();
  }

  private InfiniteScroller() {
  }

  public Observable<Object> streamPagingRequest() {
    return getScrollObservable(recyclerView, emptyListCount)
        .distinctUntilChanged()
        .cast(Object.class);
  }

  private Observable<Integer> getScrollObservable(RecyclerView recyclerView, int emptyListCount) {
    return Observable.create(emitter -> {
      final RecyclerView.OnScrollListener sl = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
          int position = getLastVisibleItemPosition(recyclerView);
          int updatePosition = (int) ((recyclerView.getAdapter().getItemCount() - 1) * scrollThreshold);

          if (position >= updatePosition) {
            int itemsAvailable = recyclerView.getAdapter().getItemCount() - emptyListCount;
            emitter.onNext(itemsAvailable);
          }
        }
      };

      recyclerView.addOnScrollListener(sl);
      emitter.setCancellable(() -> recyclerView.removeOnScrollListener(sl));
    });
  }

  private int getLastVisibleItemPosition(RecyclerView recyclerView) {
    if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
      LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
      return linearLayoutManager.findLastVisibleItemPosition();

    } else {
      throw new UnsupportedOperationException();
    }
  }

  public static class Builder {
    private RecyclerView recyclerView;
    private int emptyListCount = EMPTY_LIST_ITEMS_COUNT;
    private float scrollThreshold = DEFAULT_LOAD_THRESHOLD;

    private Builder(RecyclerView recyclerView) {
      if (recyclerView == null) {
        throw new NullPointerException("null recyclerView");
      }
      if (recyclerView.getAdapter() == null) {
        throw new NullPointerException("null recyclerView adapter");
      }
      this.recyclerView = recyclerView;
    }

    public Builder scrollThreshold(float threshold) {
      if (threshold < 0) {
        throw new AssertionError();
      }
      this.scrollThreshold = threshold;
      return this;
    }

    public Builder setEmptyListCount(int emptyListCount) {
      if (emptyListCount < 0) {
        throw new AssertionError();
      }
      this.emptyListCount = emptyListCount;
      return this;
    }

    public InfiniteScroller build() {
      InfiniteScroller infiniteScroller = new InfiniteScroller();
      infiniteScroller.recyclerView = this.recyclerView;
      infiniteScroller.emptyListCount = emptyListCount;
      infiniteScroller.scrollThreshold = scrollThreshold;
      return infiniteScroller;
    }
  }
}
