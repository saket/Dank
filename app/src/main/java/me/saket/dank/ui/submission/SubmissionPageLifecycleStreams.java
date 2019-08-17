package me.saket.dank.ui.submission;

import androidx.annotation.CheckResult;

import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import io.reactivex.Completable;
import io.reactivex.Observable;
import me.saket.dank.utils.lifecycle.LifecycleOwnerActivity;
import me.saket.dank.utils.lifecycle.LifecycleOwnerViews;
import me.saket.dank.utils.lifecycle.LifecycleStreams;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;

public class SubmissionPageLifecycleStreams extends LifecycleOwnerViews.Streams {

  private static final Object NOTHING = LifecycleStreams.NOTHING;
  private Relay<Object> pageCollapseStream = PublishRelay.create();
  private Relay<Object> pageAboutToCollapseStream = PublishRelay.create();
  private Relay<Object> pageExpandStream = PublishRelay.create();

  public static SubmissionPageLifecycleStreams create(ExpandablePageLayout pageLayout, LifecycleOwnerActivity parent) {
    return new SubmissionPageLifecycleStreams(pageLayout, parent);
  }

  public SubmissionPageLifecycleStreams(ExpandablePageLayout pageLayout, LifecycleOwnerActivity parent) {
    super(pageLayout, parent.lifecycle());

    pageLayout.addStateChangeCallbacks(new ExpandablePageLayout.StateChangeCallbacks() {
      @Override
      public void onPageAboutToExpand(long expandAnimDuration) {
      }

      @Override
      public void onPageExpanded() {
        pageExpandStream.accept(NOTHING);
      }

      @Override
      public void onPageAboutToCollapse(long collapseAnimDuration) {
        pageAboutToCollapseStream.accept(NOTHING);
      }

      @Override
      public void onPageCollapsed() {
        pageCollapseStream.accept(NOTHING);
      }
    });
  }

  @CheckResult
  public Observable<Object> onPageCollapseOrDestroy() {
    return pageCollapseStream.mergeWith(onDestroy());
  }

  @CheckResult
  public Completable onPageCollapseOrDestroyCompletable() {
    return pageCollapseStream.mergeWith(onDestroy())
        .take(1)
        .ignoreElements();
  }

  @CheckResult
  public Observable<Object> onPageCollapse() {
    return pageCollapseStream;
  }

  @CheckResult
  public Observable<Object> onPageExpand() {
    return pageExpandStream;
  }

  @CheckResult
  public Observable<Object> onPageAboutToCollapse() {
    return pageAboutToCollapseStream;
  }
}
