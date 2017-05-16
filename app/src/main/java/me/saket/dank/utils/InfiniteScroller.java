package me.saket.dank.utils;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

import android.support.annotation.Nullable;

import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.RedditObject;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.data.PaginationAnchor;
import me.saket.dank.data.exceptions.PaginationCompleteException;

/**
 * Helper class for loading infinitely scrollable {@link RedditObject RedditObjects}.
 */
public class InfiniteScroller<T extends RedditObject> {

  private final InfiniteScrollListener infiniteScrollListener;
  private Relay<StateUpdate> stateUpdateStream;
  private boolean loadFromCacheStreamOngoing;
  private boolean loadFromRemoteStreamOngoing;

  /**
   * Set to true only when a non-empty dataset is fetched.
   */
  private boolean firstDataLoadDone;

  public interface ViewStateCallbacks<T> {
    void setFirstLoadProgressVisible(boolean visible);

    void setMoreLoadProgressVisible(boolean visible);

    void setRefreshProgressVisible(boolean visible);

    void setEmptyStateVisible(boolean visible);

    void updateItemsDataset(List<T> items);

    /**
     * @param error Null when <var>boolean</var> is false.
     */
    void setErrorOnFirstLoadVisible(@Nullable Throwable error, boolean visible);

    /**
     * @param error Null when <var>boolean</var> is false.
     */
    void setErrorOnMoreLoadVisible(@Nullable Throwable error, boolean visible);
  }

  public interface InfiniteDataStreamFunction<T> {
    /**
     * @param skipCache Whether to skip the cache and hit the remote directly.
     */
    Single<List<T>> apply(PaginationAnchor paginationAnchor, boolean skipCache) throws Exception;
  }

  public static <T extends RedditObject> InfiniteScroller<T> create(InfiniteScrollListener infiniteScrollListener) {
    return new InfiniteScroller<>(infiniteScrollListener);
  }

  private InfiniteScroller(InfiniteScrollListener scrollListener) {
    infiniteScrollListener = scrollListener;
    stateUpdateStream = BehaviorRelay.<StateUpdate>create().toSerialized();
  }

  public InfiniteScroller<T> withViewStateCallbacks(ViewStateCallbacks<T> callbacks) {
    stateUpdateStream
        .startWith(new StateUpdate.InitialSetup())
        .observeOn(mainThread())
        .subscribe(updateEvent -> {
          if (updateEvent instanceof StateUpdate.InitialSetup) {
            callbacks.setFirstLoadProgressVisible(false);
            callbacks.setMoreLoadProgressVisible(false);
            callbacks.setRefreshProgressVisible(false);
            callbacks.setEmptyStateVisible(false);
            callbacks.setErrorOnFirstLoadVisible(null, false);
            callbacks.setErrorOnMoreLoadVisible(null, false);

          } else if (updateEvent instanceof StateUpdate.LoadFromCacheStart) {
            callbacks.setFirstLoadProgressVisible(((StateUpdate.LoadFromCacheStart) updateEvent).isFirstLoad);
            callbacks.setMoreLoadProgressVisible(!((StateUpdate.LoadFromCacheStart) updateEvent).isFirstLoad);

          } else if (updateEvent instanceof StateUpdate.LoadFromCacheStop) {
            callbacks.setFirstLoadProgressVisible(false);
            callbacks.setMoreLoadProgressVisible(false);

            if (loadFromRemoteStreamOngoing) {
              callbacks.setRefreshProgressVisible(true);
            }

          } else if (updateEvent instanceof StateUpdate.LoadFromRemoteStart) {
            //Timber.i("Remote load start for %s", folder);
            if (!loadFromCacheStreamOngoing) {
              callbacks.setRefreshProgressVisible(true);
            }

          } else if (updateEvent instanceof StateUpdate.LoadFromRemoteStop) {
            //Timber.i("Remote load stop for %s", folder);
            callbacks.setRefreshProgressVisible(false);

          } else if (updateEvent instanceof StateUpdate.EmptyState) {
            callbacks.setEmptyStateVisible(true);

          } else if (updateEvent instanceof StateUpdate.ItemsLoaded) {
            //noinspection unchecked
            callbacks.updateItemsDataset(((StateUpdate.ItemsLoaded<T>) updateEvent).items);
            callbacks.setEmptyStateVisible(false);

          } else if (updateEvent instanceof StateUpdate.Error) {
            boolean onFirstLoad = ((StateUpdate.Error) updateEvent).errorOnFirstLoad;
            Throwable error = ((StateUpdate.Error) updateEvent).error;
            callbacks.setErrorOnFirstLoadVisible(error, onFirstLoad);
            callbacks.setErrorOnMoreLoadVisible(error, !onFirstLoad);

            callbacks.setFirstLoadProgressVisible(false);
            callbacks.setMoreLoadProgressVisible(false);
            callbacks.setRefreshProgressVisible(false);

          } else if (updateEvent instanceof StateUpdate.PaginationComplete) {
            callbacks.setFirstLoadProgressVisible(false);
            callbacks.setMoreLoadProgressVisible(false);
            callbacks.setRefreshProgressVisible(false);

          } else {
            throw new UnsupportedOperationException("Unknown event: " + updateEvent);
          }
        });
    return this;
  }

  /**
   * Start listening to load more requests from {@link InfiniteScrollListener} as the list scrolls
   * and load more items from Reddit/cache.
   *
   * @param dataStreamFunc         For loading items from the cache/remote.
   * @param paginationAnchorStream A stream that emits a pagination anchor on every subscribe.
   *                               This anchor will be used for loading the next page of items.
   */
  public Observable<List<T>> withStreams(Single<PaginationAnchor> paginationAnchorStream, InfiniteDataStreamFunction<T> dataStreamFunc) {
    return infiniteScrollListener.emitWhenLoadNeeded()
        .flatMapSingle(o -> paginationAnchorStream)
        .scan((oldAnchor, newAnchor) -> {
          if (oldAnchor.equals(newAnchor)) {
            // No new items were fetched in the last load.
            throw new PaginationCompleteException();
          }
          return newAnchor;
        })
        .doOnNext(o -> infiniteScrollListener.setLoadOngoing(true))
        .flatMap(anchor -> loadFromDataStore(anchor, dataStreamFunc))
        .doOnNext(o -> infiniteScrollListener.setLoadOngoing(false))
        .doOnNext(messages -> {
          infiniteScrollListener.setLoadOngoing(false);

          if (messages.isEmpty()) {
            stateUpdateStream.accept(new StateUpdate.EmptyState());
          } else {
            firstDataLoadDone = true;
            stateUpdateStream.accept(new StateUpdate.ItemsLoaded<>(messages));
          }

          if (messages.isEmpty()) {
            // First load, no messages received. Inbox folder is empty.
            throw new PaginationCompleteException();
          }
        })
        .onErrorResumeNext(error -> {
          if (!(error instanceof PaginationCompleteException)) {
            // Note: the remote stream does not emit any errors.
            boolean errorOnFirstLoad = !firstDataLoadDone;
            stateUpdateStream.accept(new StateUpdate.Error(error, errorOnFirstLoad));
          } else {
            stateUpdateStream.accept(new StateUpdate.PaginationComplete());
          }

          return Observable.never();
        });
  }

  /**
   * Load data from cache and force-refresh from remote. Remote data is emitted only if it's new.
   */
  private Observable<List<T>> loadFromDataStore(PaginationAnchor anchor, InfiniteDataStreamFunction<T> dataStreamFunc) throws Exception {
    Observable<List<T>> cacheStream = dataStreamFunc.apply(anchor, false /* skipCache */)
        .subscribeOn(Schedulers.io())
        .doOnSubscribe(o -> {
          loadFromCacheStreamOngoing = true;
          boolean isFirstLoad = !firstDataLoadDone;
          stateUpdateStream.accept(new StateUpdate.LoadFromCacheStart(isFirstLoad));
        })
        .doOnSuccess(o -> {
          loadFromCacheStreamOngoing = false;
          stateUpdateStream.accept(new StateUpdate.LoadFromCacheStop());
        })
        .doOnError(o -> {
          loadFromCacheStreamOngoing = false;
          stateUpdateStream.accept(new StateUpdate.LoadFromCacheStop());
        })
        .toObservable();

    // We'll do a force refresh only on first load.
    Observable<List<T>> refreshStream;
    if (anchor.isEmpty()) {
      refreshStream = dataStreamFunc.apply(anchor, true /* skipCache */)
          .subscribeOn(Schedulers.io())
          .doOnSubscribe(o -> {
            loadFromRemoteStreamOngoing = true;
            stateUpdateStream.accept(new StateUpdate.LoadFromRemoteStart());
          })
          .doOnSuccess(o -> {
            loadFromRemoteStreamOngoing = false;
            stateUpdateStream.accept(new StateUpdate.LoadFromRemoteStop());
          })
          .doOnError(o -> {
            loadFromRemoteStreamOngoing = false;
            stateUpdateStream.accept(new StateUpdate.LoadFromRemoteStop());
          })
          .onErrorResumeNext(e -> {
            e.printStackTrace();
            return Single.never();
          })
          .toObservable();
    } else {
      refreshStream = Observable.never();
    }

    // Note: It is necessary that the two streams run in separate threads
    // otherwise Observable.merge() will act like concat().
    //noinspection unchecked
    return Observable
        .mergeDelayError(cacheStream, refreshStream)
        .distinctUntilChanged()
        .observeOn(mainThread());
  }

  private interface StateUpdate {
    class InitialSetup implements StateUpdate {}

    class LoadFromCacheStart implements StateUpdate {
      public boolean isFirstLoad;

      public LoadFromCacheStart(boolean isFirstLoad) {
        this.isFirstLoad = isFirstLoad;
      }
    }

    class LoadFromCacheStop implements StateUpdate {}

    class LoadFromRemoteStart implements StateUpdate {}

    class LoadFromRemoteStop implements StateUpdate {}

    class ItemsLoaded<T> implements StateUpdate {
      private List<T> items;

      public ItemsLoaded(List<T> items) {
        this.items = items;
      }
    }

    class EmptyState implements StateUpdate {}

    class Error implements StateUpdate {
      private Throwable error;
      private boolean errorOnFirstLoad;

      public Error(Throwable error, boolean errorOnFirstLoad) {
        this.error = error;
        this.errorOnFirstLoad = errorOnFirstLoad;
      }
    }

    class PaginationComplete implements StateUpdate {}
  }

}
