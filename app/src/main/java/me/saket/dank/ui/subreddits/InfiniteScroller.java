package me.saket.dank.ui.subreddits;


import android.os.Looper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.jakewharton.rxbinding2.internal.Notification;

import io.reactivex.Observable;

/**
 * @author https://github.com/matzuk/PaginationSample
 */
public class InfiniteScroller {

  private static final Object DUMMY = Notification.INSTANCE;

  // Fetch more items once the list has scrolled past 75% of its items.
  public static final float DEFAULT_LOAD_THRESHOLD = 0.65f;

  private RecyclerView recyclerView;
  private float scrollThreshold;

  public static Observable<Object> streamPagingRequests(RecyclerView recyclerView) {
    return new Builder(recyclerView).build().streamPagingRequest();
  }

  private InfiniteScroller() {
  }

  public Observable<Object> streamPagingRequest() {
    return getScrollObservable(recyclerView)
        .distinctUntilChanged()
        .cast(Object.class);
  }

  private Observable<Object> getScrollObservable(RecyclerView recyclerView) {
    return Observable.create(emitter -> {
      if (Looper.myLooper() != Looper.getMainLooper()) {
        throw new IllegalStateException(
            "Expected to be called on the main thread but was " + Thread.currentThread().getName());
      }

      final RecyclerView.OnScrollListener sl = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
          int position = getLastVisibleItemPosition(recyclerView);
          int updatePosition = (int) ((recyclerView.getAdapter().getItemCount() - 1) * scrollThreshold);

          if (position >= updatePosition && updatePosition != 0) {
            //Timber.i("------------------");
            //Timber.i("position: %s", position);
            //Timber.i("updatePosition: %s", updatePosition);
            emitter.onNext(DUMMY);
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

    public InfiniteScroller build() {
      InfiniteScroller infiniteScroller = new InfiniteScroller();
      infiniteScroller.recyclerView = this.recyclerView;
      infiniteScroller.scrollThreshold = scrollThreshold;
      return infiniteScroller;
    }
  }
}
