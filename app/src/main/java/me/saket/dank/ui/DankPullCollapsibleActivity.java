package me.saket.dank.ui;

import static me.saket.dank.utils.Views.executeOnMeasure;

import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.view.MenuItem;
import android.view.View;

import me.saket.dank.R;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import me.saket.dank.widgets.StatusBarBackgroundLayout;

/**
 * An Activity that can be dismissed by pulling it vertically.
 * <p>
 * Checklist for subclasses:
 * 1. Call {@link #setPullToCollapseEnabled(boolean)} if needed before super.onCreate().
 * 2. Call {@link #setupContentExpandablePage(IndependentExpandablePageLayout)}.
 * 3. Finally, call {@link #expandFromBelowToolbar()} or {@link #expandFrom(Rect)}.
 */
public abstract class DankPullCollapsibleActivity extends DankActivity {

  public static final String KEY_EXPAND_FROM_SHAPE = "expandFromShape";

  private IndependentExpandablePageLayout activityPageLayout;
  private Rect expandedFromRect;
  private int activityParentToolbarHeight;
  private boolean pullCollapsibleEnabled = true;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (pullCollapsibleEnabled) {
      overridePendingTransition(0, 0);
    }

    TypedArray typedArray = obtainStyledAttributes(new int[] { android.R.attr.actionBarSize });
    activityParentToolbarHeight = typedArray.getDimensionPixelSize(0, 0);
    typedArray.recycle();
  }

  @Override
  public void setContentView(@LayoutRes int layoutResID) {
    if (pullCollapsibleEnabled) {
      StatusBarBackgroundLayout wrapperLayout = new StatusBarBackgroundLayout(this, R.color.toolbar);
      View.inflate(this, layoutResID, wrapperLayout);
      super.setContentView(wrapperLayout);

    } else {
      super.setContentView(layoutResID);
    }
  }

  /**
   * Defaults to true. When disabled, this behaves like a normal Activity with no expandable page animations.
   * Should be called before onCreate().
   */
  protected void setPullToCollapseEnabled(boolean enabled) {
    pullCollapsibleEnabled = enabled;
  }

  /**
   * I've considered automatically wrapping layouts in an {@link IndependentExpandablePageLayout} so that this
   * extra step of boilerplate is removed, but inflation is expensive and the aim was to minimize startup time
   * because unlike Activity transitions which starts as soon as an Activity is created, our entry animation
   * runs after the Activity is ready.
   */
  protected void setupContentExpandablePage(IndependentExpandablePageLayout pageLayout) {
    activityPageLayout = pageLayout;

    if (pullCollapsibleEnabled) {
      activityPageLayout.setPullToCollapseDistanceThreshold(activityParentToolbarHeight);
      pageLayout.setCallbacks(new IndependentExpandablePageLayout.Callbacks() {
        @Override
        public void onPageFullyCollapsed() {
          DankPullCollapsibleActivity.super.finish();
          overridePendingTransition(0, 0);
        }

        @Override
        public void onPageRelease(boolean collapseEligible) {
          if (collapseEligible) {
            finish();
          }
        }
      });

    } else {
      activityPageLayout.setPullToCollapseEnabled(false);
      activityPageLayout.expandImmediately();
    }
  }

  protected void expandFromBelowToolbar() {
    executeOnMeasure(activityPageLayout, () -> {
      Rect toolbarRect = new Rect(0, activityParentToolbarHeight + Views.statusBarHeight(getResources()), activityPageLayout.getWidth(), 0);
      expandFrom(toolbarRect);
    });
  }

  protected void expandFrom(@Nullable Rect fromRect) {
    if (fromRect == null) {
      expandFromBelowToolbar();
    }

    expandedFromRect = fromRect;
    executeOnMeasure(activityPageLayout, () -> {
      activityPageLayout.expandFrom(fromRect);
    });
  }

  @Override
  public void finish() {
    if (pullCollapsibleEnabled) {
      activityPageLayout.collapseTo(expandedFromRect);
    } else {
      super.finish();
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      onBackPressed();
      return true;

    } else {
      return super.onOptionsItemSelected(item);
    }
  }

}
