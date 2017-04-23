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
public class InfiniteScroller {

    private final float loadMoreOffsetFactor;
    private final RecyclerView recyclerView;
    private boolean isLoadOngoing;

    /**
     * @param loadMoreOffsetFactor %age of items after which new items should be loaded. For eg., with 20 items
     *                             and a factor of 0.6f, a new load will be triggered when the list reaches the
     *                             12th item.
     */
    public InfiniteScroller(RecyclerView recyclerView, @FloatRange(from = 0f, to = 1f) float loadMoreOffsetFactor) {
        this.loadMoreOffsetFactor = loadMoreOffsetFactor;
        this.recyclerView = recyclerView;
    }

    @CheckResult
    public Observable<?> emitWhenLoadNeeded() {
        // RxRecyclerView doesn't emit an initial value, unlike all other bindings. So do it manually.
        RecyclerViewScrollEvent initialScrollEvent = RecyclerViewScrollEvent.create(recyclerView, 0, 0);

        return RxRecyclerView
                .scrollEvents(recyclerView)
                //.toFlowable(BackpressureStrategy.LATEST)
                .startWith(initialScrollEvent)
                .filter(shouldLoadMore());
    }

    public void setLoadOngoing(boolean loadOngoing) {
        isLoadOngoing = loadOngoing;
    }

    private Predicate<RecyclerViewScrollEvent> shouldLoadMore() {
        return scrollEvent -> {
            if (isLoadOngoing) {
                return false;
            }

            RecyclerView recyclerView = scrollEvent.view();
            int totalItemCount = recyclerView.getAdapter().getItemCount();
            int lastVisibleItemPosition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();

            float threshold = (float) totalItemCount * loadMoreOffsetFactor;
            return lastVisibleItemPosition + 1 >= threshold;
        };
    }

}
