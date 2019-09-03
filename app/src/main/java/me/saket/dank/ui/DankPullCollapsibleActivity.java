package me.saket.dank.ui;

import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;

import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

import static me.saket.dank.utils.Views.executeOnMeasure;

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
  private boolean wasActivityRecreated;
  private boolean entryAnimationEanbled = true;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    wasActivityRecreated = savedInstanceState == null;

    if (entryAnimationEanbled && pullCollapsibleEnabled) {
      overridePendingTransition(0, 0);
    }

    TypedArray typedArray = obtainStyledAttributes(new int[] { android.R.attr.actionBarSize });
    activityParentToolbarHeight = typedArray.getDimensionPixelSize(0, 0);
    typedArray.recycle();
  }

  public void setEntryAnimationEnabled(boolean entryAnimationEnabled) {
    this.entryAnimationEanbled = entryAnimationEnabled;
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
      Rect toolbarRect = new Rect(0, activityParentToolbarHeight, activityPageLayout.getWidth(), 0);
      expandFrom(toolbarRect);
    });
  }

  protected void expandFrom(@Nullable Rect fromRect) {
    if (fromRect == null) {
      expandFromBelowToolbar();
      return;
    }

    expandedFromRect = fromRect;
    executeOnMeasure(activityPageLayout, () -> {
      if (wasActivityRecreated) {
        activityPageLayout.expandFrom(fromRect);
      } else {
        activityPageLayout.expandImmediately();
      }
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
      finish();
      return true;

    } else {
      return super.onOptionsItemSelected(item);
    }
  }
}
