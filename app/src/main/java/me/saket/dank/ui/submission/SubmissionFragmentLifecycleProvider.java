package me.saket.dank.ui.submission;

import com.jakewharton.rxbinding2.internal.Notification;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;

public class SubmissionFragmentLifecycleProvider {

  private Relay<Object> pageCollapseStream = PublishRelay.create();
  private Relay<Object> pageAboutToCollapseStream = PublishRelay.create();

  public SubmissionFragmentLifecycleProvider(ExpandablePageLayout pageLayout) {
    pageLayout.addStateChangeCallbacks(new ExpandablePageLayout.StateChangeCallbacks() {
      @Override
      public void onPageAboutToExpand(long expandAnimDuration) {

      }

      @Override
      public void onPageExpanded() {

      }

      @Override
      public void onPageAboutToCollapse(long collapseAnimDuration) {
        pageAboutToCollapseStream.accept(Notification.INSTANCE);
      }

      @Override
      public void onPageCollapsed() {
        pageCollapseStream.accept(Notification.INSTANCE);
      }
    });
  }

  public Relay<Object> onPageCollapse() {
    return pageCollapseStream;
  }

  public Relay<Object> onPageAboutToCollapse() {
    return pageAboutToCollapseStream;
  }
}
