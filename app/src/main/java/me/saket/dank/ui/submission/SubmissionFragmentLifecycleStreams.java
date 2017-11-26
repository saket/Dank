package me.saket.dank.ui.submission;

import android.support.annotation.CheckResult;

import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import me.saket.dank.data.ActivityResult;
import me.saket.dank.utils.lifecycle.LifecycleStreams;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;

public class SubmissionFragmentLifecycleStreams implements LifecycleStreams {

  private final LifecycleStreams delegate;
  private Relay<Object> pageCollapseStream = PublishRelay.create();
  private Relay<Object> pageAboutToCollapseStream = PublishRelay.create();

  public static SubmissionFragmentLifecycleStreams wrap(LifecycleStreams delegate, ExpandablePageLayout layout) {
    return new SubmissionFragmentLifecycleStreams(delegate, layout);
  }

  public SubmissionFragmentLifecycleStreams(LifecycleStreams delegate, ExpandablePageLayout pageLayout) {
    this.delegate = delegate;

    pageLayout.addStateChangeCallbacks(new ExpandablePageLayout.StateChangeCallbacks() {
      @Override
      public void onPageAboutToExpand(long expandAnimDuration) {}

      @Override
      public void onPageExpanded() {}

      @Override
      public void onPageAboutToCollapse(long collapseAnimDuration) {
        pageAboutToCollapseStream.accept(LifecycleStreams.NOTHING);
      }

      @Override
      public void onPageCollapsed() {
        pageCollapseStream.accept(LifecycleStreams.NOTHING);
      }
    });
  }

  @CheckResult
  public Observable<Object> onPageCollapseOrDestroy() {
    return pageCollapseStream.mergeWith(onDestroy());
  }

  @CheckResult
  public Observable<Object> onPageAboutToCollapse() {
    return pageAboutToCollapseStream;
  }

  @Override
  public Observable<Object> onStart() {
    return delegate.onStart();
  }

  @Override
  public Observable<Object> onResume() {
    return delegate.onResume();
  }

  @Override
  public Observable<Object> onPause() {
    return delegate.onPause();
  }

  @Override
  public Observable<Object> onStop() {
    return delegate.onStop();
  }

  @Override
  public Observable<Object> onDestroy() {
    return delegate.onDestroy();
  }

  @Override
  public Flowable<Object> onDestroyFlowable() {
    return delegate.onDestroyFlowable();
  }

  @Override
  public Observable<ActivityResult> onActivityResults() {
    return delegate.onActivityResults();
  }
}
