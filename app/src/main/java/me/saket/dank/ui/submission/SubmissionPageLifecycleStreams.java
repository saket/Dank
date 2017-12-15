package me.saket.dank.ui.submission;

import android.support.annotation.CheckResult;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;
import io.reactivex.Observable;
import me.saket.dank.utils.lifecycle.LifecycleOwnerViews.ViewLifecycleStreams;
import me.saket.dank.utils.lifecycle.LifecycleStreams;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;

public class SubmissionPageLifecycleStreams extends ViewLifecycleStreams {

  private Relay<Object> pageCollapseStream = PublishRelay.create();
  private Relay<Object> pageAboutToCollapseStream = PublishRelay.create();

  public static SubmissionPageLifecycleStreams wrap(ExpandablePageLayout pageLayout, ViewLifecycleStreams delegate) {
    return new SubmissionPageLifecycleStreams(pageLayout, delegate);
  }

  public SubmissionPageLifecycleStreams(ExpandablePageLayout pageLayout, LifecycleStreams delegate) {
    super(pageLayout, delegate);

    pageLayout.addStateChangeCallbacks(new ExpandablePageLayout.StateChangeCallbacks() {
      @Override
      public void onPageAboutToExpand(long expandAnimDuration) {
      }

      @Override
      public void onPageExpanded() {
      }

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
  public Observable<Object> onPageCollapse() {
    return pageCollapseStream;
  }

  @CheckResult
  public Observable<Object> onPageAboutToCollapse() {
    return pageAboutToCollapseStream;
  }
}
