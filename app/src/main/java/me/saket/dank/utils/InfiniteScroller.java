package me.saket.dank.utils;


import android.os.Looper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.reactivex.Observable;

/**
 * Usage: RV's adpater must implement {@link InfinitelyScrollableRecyclerViewAdapter}.
 *
 * @author https://github.com/matzuk/PaginationSample
 */
public class InfiniteScroller {

  public static final float DEFAULT_SCROLL_THRESHOLD = 0.65f;

  private RecyclerView recyclerView;
  private float scrollThreshold;

  public static Observable<Object> streamPagingRequests(RecyclerView recyclerView) {
    return new InfiniteScroller(recyclerView, DEFAULT_SCROLL_THRESHOLD).streamPagingRequest();
  }

  private InfiniteScroller(RecyclerView recyclerView, float scrollThreshold) {
    this.recyclerView = recyclerView;
    this.scrollThreshold = scrollThreshold;
  }

  public Observable<Object> streamPagingRequest() {
    Observable<Integer> scrollObservable = Observable.<Integer>create(emitter -> {
      if (Looper.myLooper() != Looper.getMainLooper()) {
        throw new IllegalStateException("Expected to be called on the main thread but was " + Thread.currentThread().getName());
      }

      final RecyclerView.OnScrollListener sl = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView rv, int dx, int dy) {
          if (recyclerView.getAdapter() == null) {
            return;
          }
          if (!(recyclerView.getAdapter() instanceof InfinitelyScrollableRecyclerViewAdapter)) {
            throw new AssertionError("RecyclerView adapter must implement InfinitelyScrollableRecyclerViewAdapter");
          }

          int position = getLastVisibleItemPosition(recyclerView);
          // This stream has a distinctUntilChanged() applied to it. The item count has to be stable
          // even when more items are being fetched or else another request will get triggered.
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

    return scrollObservable
        .distinctUntilChanged()
        .cast(Object.class);
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
