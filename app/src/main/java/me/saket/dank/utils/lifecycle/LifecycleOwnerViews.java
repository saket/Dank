package me.saket.dank.utils.lifecycle;

import androidx.annotation.CheckResult;
import android.view.View;

import com.jakewharton.rxrelay2.BehaviorRelay;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import me.saket.dank.data.ActivityResult;

/**
 * This class exists instead of just {@link Streams} to keep it consistent with
 * {@link LifecycleOwnerActivity}, {@link LifecycleOwnerFragment}, etc.
 */
public class LifecycleOwnerViews {

  /**
   * @param streams From parent Activity or fragment.
   */
  public static Streams create(View view, ActivityLifecycleStreams streams) {
    return new Streams(view, streams);
  }

  public static class Streams implements LifecycleStreams<ViewLifecycleEvent> {
    private final ActivityLifecycleStreams parentStreams;
    private final Observable<ViewLifecycleEvent> events;
    private final BehaviorRelay<ViewLifecycleEvent> replayedEvents = BehaviorRelay.create();

    public Streams(View view, ActivityLifecycleStreams parentStreams) {
      this.parentStreams = parentStreams;

      Observable<ViewLifecycleEvent> viewEvents = Observable.create(emitter -> {
        View.OnAttachStateChangeListener listener = new View.OnAttachStateChangeListener() {
          @Override
          public void onViewAttachedToWindow(View v) {
            emitter.onNext(ViewLifecycleEvent.ATTACH);
          }

          @Override
          public void onViewDetachedFromWindow(View v) {
            emitter.onNext(ViewLifecycleEvent.DETACH);
          }
        };

        view.addOnAttachStateChangeListener(listener);
        emitter.setCancellable(() -> view.removeOnAttachStateChangeListener(listener));
      });

      Observable<ViewLifecycleEvent> activityEvents = parentStreams.events()
          .map(activityLifecycleEvent -> {
            switch (activityLifecycleEvent) {
              case START:
                return ViewLifecycleEvent.START;

              case STOP:
                return ViewLifecycleEvent.STOP;

              case RESUME:
                return ViewLifecycleEvent.RESUME;

              case PAUSE:
                return ViewLifecycleEvent.PAUSE;

              case DESTROY:
                return ViewLifecycleEvent.DESTROY;

              default:
                throw new UnsupportedOperationException("Unknown activity lifecycle event: " + activityLifecycleEvent);
            }
          });

      events = viewEvents
          .mergeWith(activityEvents)
          .takeUntil(viewEvents.filter(e -> e == ViewLifecycleEvent.DESTROY))
          .share();

      events
          .takeUntil(onDestroy())
          .subscribe(replayedEvents);
    }

    @CheckResult
    public Observable<ViewLifecycleEvent> viewAttaches() {
      return events.filter(e -> e == ViewLifecycleEvent.ATTACH);
    }

    @CheckResult
    public Observable<ViewLifecycleEvent> viewDetaches() {
      return events.filter(e -> e == ViewLifecycleEvent.DETACH).take(1);
    }

    @CheckResult
    public Flowable<ViewLifecycleEvent> viewDetachesFlowable() {
      return viewDetaches().toFlowable(BackpressureStrategy.LATEST);
    }

    @CheckResult
    public Completable viewDetachesCompletable() {
      return viewDetaches().ignoreElements();
    }

    @Override
    public Observable<ViewLifecycleEvent> events() {
      return events;
    }

    @CheckResult
    public Observable<ViewLifecycleEvent> onStart() {
      return events.filter(e -> e == ViewLifecycleEvent.START);
    }

    @CheckResult
    public Observable<ViewLifecycleEvent> onResume() {
      return events.filter(e -> e == ViewLifecycleEvent.RESUME);
    }

    @CheckResult
    public Observable<ViewLifecycleEvent> onPause() {
      return events.filter(e -> e == ViewLifecycleEvent.PAUSE);
    }

    @CheckResult
    public Observable<ViewLifecycleEvent> onStop() {
      return events.filter(e -> e == ViewLifecycleEvent.STOP);
    }

    @CheckResult
    public Observable<ViewLifecycleEvent> onDestroy() {
      return events.filter(e -> e == ViewLifecycleEvent.DESTROY).take(1);
    }

    @CheckResult
    public Observable<ActivityResult> onActivityResults() {
      return parentStreams.onActivityResults();
    }

    @CheckResult
    public Observable<ViewLifecycleEvent> replayedEvents() {
      return replayedEvents;
    }
  }
}
