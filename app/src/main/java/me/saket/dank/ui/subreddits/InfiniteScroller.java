package me.saket.dank.ui.subreddits;


import android.os.Looper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import io.reactivex.Observable;

/**
 * Usage: RV's adpater must implement {@link InfinitelyScrollableRecyclerViewAdapter}.
 *
 * @author https://github.com/matzuk/PaginationSample
 */
public class InfiniteScroller {

  // Fetch more items once the list has scrolled past 75% of its items.
  public static final float DEFAULT_SCROLL_THRESHOLD = 0.65f;

  private RecyclerView recyclerView;
  private float scrollThreshold;

  public static Observable<Object> streamPagingRequests(RecyclerView recyclerView) {
    return new InfiniteScroller(recyclerView, DEFAULT_SCROLL_THRESHOLD).streamPagingRequest();
  }

  private InfiniteScroller(RecyclerView recyclerView, float scrollThreshold) {
    if (!(recyclerView.getAdapter() instanceof InfinitelyScrollableRecyclerViewAdapter)) {
      throw new AssertionError("RecyclerView adapter must implement InfinitelyScrollableRecyclerViewAdapter");
    }

    this.recyclerView = recyclerView;
    this.scrollThreshold = scrollThreshold;
  }

  public Observable<Object> streamPagingRequest() {
    return getScrollObservable(recyclerView)
        .distinctUntilChanged()
        .cast(Object.class);
  }

  private Observable<Integer> getScrollObservable(RecyclerView recyclerView) {
    return Observable.create(emitter -> {
      if (Looper.myLooper() != Looper.getMainLooper()) {
        throw new IllegalStateException(
            "Expected to be called on the main thread but was " + Thread.currentThread().getName());
      }

      final RecyclerView.OnScrollListener sl = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
          int position = getLastVisibleItemPosition(recyclerView);
          int adapterDatasetCount = ((InfinitelyScrollableRecyclerViewAdapter) recyclerView.getAdapter()).getItemCountMinusDecorators();
          int updatePosition = (int) ((adapterDatasetCount - 1) * scrollThreshold);

          if (position >= updatePosition && updatePosition != 0) {
            //Timber.i("------------------");
            //Timber.i("position: %s", position);
            //Timber.i("updatePosition: %s", updatePosition);
            emitter.onNext(adapterDatasetCount);
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

      // Commented because I haven't been able to figure out how to setup StaggeredGridLayoutManager.
//    } else if (recyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {
//      StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
//      int[] lastVisibleRowPositions = staggeredGridLayoutManager.findLastVisibleItemPositions(null);
//      Timber.i("lastVisibleRowPositions: %s", Arrays.toString(lastVisibleRowPositions));
//      return Arrays2.max(lastVisibleRowPositions);

    } else {
      throw new UnsupportedOperationException("Unknown LayoutManager.");
    }
  }
}
