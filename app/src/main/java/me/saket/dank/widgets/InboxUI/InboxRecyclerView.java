package me.saket.dank.widgets.InboxUI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.Window;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout.PageState;

/**
 * Mimics the expandable layout in the Inbox app by Google. #AcheDin.
 */
public class InboxRecyclerView extends RecyclerView implements ExpandablePageLayout.InternalPageCallbacks {

  private static final String KEY_IS_EXPANDED = "isExpanded";
  private static final float MAX_DIM_FACTOR = 0.2f;                    // [0..1]
  private static final int MAX_DIM = (int) (255 * MAX_DIM_FACTOR);     // [0..255]

  private ExpandablePageLayout page;
  private ExpandInfo expandInfo;             // Details about the currently expanded Item
  private Paint dimPaint;
  private Window activityWindow;
  private Drawable activityWindowOrigBackground;
  private boolean pendingItemsOutOfTheWindowAnimation;
  private boolean isFullyCoveredByPage;
  private boolean layoutManagerCreated;

  public InboxRecyclerView(Context context) {
    super(context);
    init();
  }

  public InboxRecyclerView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public InboxRecyclerView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init();
  }

  private void init() {
    // For drawing an overlay shadow while the expandable page is fully expanded.
    setWillNotDraw(false);
    dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    dimPaint.setColor(Color.BLACK);
    dimPaint.setAlpha(MAX_DIM);
  }

  public LayoutManager createLayoutManager() {
    layoutManagerCreated = true;
    return new LinearLayoutManager(getContext()) {
      @Override
      public int scrollVerticallyBy(int dy, Recycler recycler, State state) {
        return !canScroll() ? 0 : super.scrollVerticallyBy(dy, recycler, state);
      }
    };
  }

  public void saveExpandableState(Bundle outState) {
    if (page != null) {
      outState.putBoolean(KEY_IS_EXPANDED, page.isExpanded());
    }
  }

  /** Letting Activities handle restoration manually so that the setup can happen before onRestore gets called. */
  public void restoreExpandableState(Bundle savedInstance) {
    boolean wasExpanded = savedInstance.getBoolean(KEY_IS_EXPANDED);
    if (wasExpanded) {
      if (page == null) {
        throw new NullPointerException("setExpandablePage() must be called before handleOnRetainInstance()");
      }
      page.expandImmediately();
    }
  }

  /**
   * Sets the {@link ExpandablePageLayout} and {@link Toolbar} to be used with this list. The toolbar
   * gets pushed up when the page is expanding. It is also safe to call this method again and replace
   * the ExpandablePage or Toolbar.
   */
  public void setExpandablePage(ExpandablePageLayout expandablePage, View toolbar) {
    page = expandablePage;
    expandablePage.setup(toolbar);
    expandablePage.setInternalStateCallbacksForList(this);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);

    // In case any expand() call was made before this list and its child Views were measured, perform it now.
    if (pendingItemsOutOfTheWindowAnimation) {
      pendingItemsOutOfTheWindowAnimation = false;
      animateItemsOutOfTheWindow(true);
    }
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    // Must have gotten called because the keyboard was called / hidden. The items must maintain their
    // positions, relative to the new bounds. Wait for Android to draw the child Views. Calling
    // getChildCount() right now will return old values (that is, no. of children that were present before
    // this height change happened.
    if (page == null) {
      return;
    }

    Views.executeOnNextLayout(this, () -> {
      // Fix list items.
      if (getPage().isExpandedOrExpanding()) {
        animateItemsOutOfTheWindow(getPage().isExpanded());
      } else {
        animateItemsBackToPosition(getPage().isCollapsed());
      }

      // Fix expandable page (or else it gets stuck in the middle since it doesn't know of the size change).
      if (getPage().getCurrentState() == PageState.EXPANDING) {
        getPage().animatePageExpandCollapse(true, getWidth(), getHeight(), getExpandInfo());

      } else if (getPage().getCurrentState() == PageState.EXPANDED) {
        getPage().alignPageToCoverScreen();
      }
    });
  }

// ======== EXPAND / COLLAPSE ======== //

  /**
   * @param itemViewPosition Item's position in the RecyclerView. This is not the same as adapter position.
   */
  public void expandItem(int itemViewPosition, long itemId) {
    if (page == null) {
      throw new IllegalAccessError("You must call InboxRecyclerView.setup(ExpandablePage, Toolbar)");
    }
    if (!layoutManagerCreated) {
      throw new IllegalAccessError("LayoutManager isn't set. #Use createLayoutManager()");
    }

    // Ignore if already expanded.
    if (getPage().isExpandedOrExpanding()) {
      return;
    }

    View child = getChildAt(itemViewPosition);
    if (child == null) {
      throw new IllegalArgumentException("No item at the specified position in InboxRecyclerView: " + itemViewPosition);
    }

    // Store these details so that they can be used later for restoring the original state.
    final Rect itemRect = new Rect(
        getLeft() + child.getLeft(),
        getTop() + child.getTop(),
        (getWidth() - getRight()) + child.getRight(),
        getTop() + child.getBottom()
    );

    if (itemRect.width() == 0) {
      // Should expand from full width even when expanding from arbitrary location (that is, item to expand is null).
      itemRect.left = getLeft();
      itemRect.right = getRight();
    }
    expandInfo = new ExpandInfo(itemViewPosition, itemId, itemRect);

    // Animate all items out of the window and expand the page.
    animateItemsOutOfTheWindow();

    // Skip animations if Android hasn't measured Views yet.
    if (!isLaidOut() && getVisibility() != GONE) {
      expandImmediately();
    } else {
      getPage().expand(getExpandInfo());
    }
  }

  /**
   * Expands from arbitrary location. Presently the top.
   *
   * @deprecated This will possibly crash in expandItem(). Fix before using.
   */
  public void expandFromTop() {
    expandItem(-1, -1);
  }

  /**
   * Expands the page right away and pushes the items out of the list without animations.
   */
  public void expandImmediately() {
    getPage().expandImmediately();

    // This will push all the list items to the bottom, as if the item
    // above the 0th position was expanded
    animateItemsOutOfTheWindow(true);
  }

  public void collapse() {
    if (getPage() == null) {
      throw new IllegalStateException("No page attached. Cannot collapse. ListId: " + getId());
    }

    // Ignore if already collapsed
    if (getPage().isCollapsedOrCollapsing()) {
      return;
    }
    pendingItemsOutOfTheWindowAnimation = false;

    // This ensures the items were present outside the window before collapse starts
    if (getPage().getTranslationY() == 0) {
      animateItemsOutOfTheWindow(true);
    }

    // Collapse the page and restore the item positions of this list
    if (getPage() != null) {
      ExpandInfo expandInfo = getExpandInfo();
      getPage().collapse(expandInfo);
    }
    animateItemsBackToPosition(false);
  }

// ======== ANIMATION ======== //

  /**
   * Animates all items out of the Window. The item at position <code>expandInfo.expandedItemPosition</code>
   * is moved to the top, while the items above it are animated out of the window from the top and the rest
   * from the bottom.
   */
  void animateItemsOutOfTheWindow(boolean immediate) {
    if (!isLaidOut()) {
      // Neither this list has been drawn yet nor its child views.
      pendingItemsOutOfTheWindowAnimation = true;
      return;
    }

    final int anchorPosition = getExpandInfo().expandedItemPosition;
    final int listHeight = getHeight();

    for (int i = 0; i < getChildCount(); i++) {
      final View view = getChildAt(i);

      // 1. Anchor view to the top edge
      // 2. Views above it out of the top edge
      // 3. Views below it out of the bottom edge
      final float moveY;
      final boolean above = i <= anchorPosition;

      if (anchorPosition == -1 || view.getHeight() <= 0) {
        // Item to expand not present in the list. Send all Views outside the bottom edge
        moveY = listHeight - getPaddingTop();

      } else {
        final int positionOffset = i - anchorPosition;
        moveY = above
            ? -view.getTop() + positionOffset * view.getHeight()
            : listHeight - view.getTop() + view.getHeight() * (positionOffset - 1);
      }

      view.animate().cancel();
      if (!immediate) {
        view.animate()
            .translationY(moveY)
            .setDuration(page.getAnimationDurationMillis())
            .setInterpolator(page.getAnimationInterpolator())
            .setStartDelay(getAnimationStartDelay());

        if (anchorPosition == i) {
          view.animate().alpha(0f).withLayer();
        }

      } else {
        //noinspection ResourceType
        view.setTranslationY(moveY);
        if (anchorPosition == i) {
          view.setAlpha(0f);
        }
      }
    }
  }

  void animateItemsOutOfTheWindow() {
    animateItemsOutOfTheWindow(false);
  }

  /**
   * Reverses animateItemsOutOfTheWindow() by moving all items back to their actual positions.
   */
  protected void animateItemsBackToPosition(boolean immediate) {
    int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      View view = getChildAt(i);
      if (view == null) {
        continue;
      }

      // Strangely, both the sections (above and below) are getting restored at the same time even when
      // the animation duration is same. :O
      // Update: Oh god. I confused time with speed. Not deleting this so that this comment always
      // reminds me how stupid I can be at times.
      view.animate().cancel();

      if (!immediate) {
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(page.getAnimationDurationMillis())
            .setInterpolator(page.getAnimationInterpolator())
            .setStartDelay(getAnimationStartDelay());

      } else {
        view.setTranslationY(0f);
        view.setAlpha(1f);
      }
    }
  }

// ======== PAGE CALLBACKS ======== //

  @Override
  public void onPageAboutToCollapse() {
    onPageBackgroundVisible();
    postInvalidate();
  }

  @Override
  public void onPageFullyCollapsed() {
    expandInfo = null;
  }

  @Override
  public void onPagePull(float deltaY) {
    for (int i = 0; i < getChildCount(); i++) {
      final View itemView = getChildAt(i);

      // Stop any ongoing animation in case the user started pulling while the list items were
      // still animating (out of the window).
      itemView.animate().cancel();
      itemView.setTranslationY(itemView.getTranslationY() + deltaY);
    }
    onPageBackgroundVisible();
  }

  @Override
  public void onPageRelease(boolean collapseEligible) {
    if (collapseEligible) {
      collapse();
      onPageBackgroundVisible();
    } else {
      animateItemsOutOfTheWindow();
    }
  }

  @Override
  public void onPageFullyCovered() {
    final boolean invalidate = !isFullyCoveredByPage;
    isFullyCoveredByPage = true;   // Skips draw() until visible again to the user.
    if (invalidate) {
      postInvalidate();
    }

    if (activityWindow != null) {
      activityWindow.setBackgroundDrawable(null);
    }
  }

  public void onPageBackgroundVisible() {
    final boolean invalidate = isFullyCoveredByPage;
    isFullyCoveredByPage = false;
    if (invalidate) {
      postInvalidate();
    }

    if (activityWindow != null) {
      activityWindow.setBackgroundDrawable(activityWindowOrigBackground);
    }
  }

// ======== DIMMING + REDUCING OVERDRAW ======== //

  @Override
  public void draw(Canvas canvas) {
    // Minimize overdraw by not drawing anything while this list is totally covered by the expandable page.
    if (isFullyCoveredByPage) {
      return;
    }

    super.draw(canvas);

    if (page != null && page.isExpanded()) {
      canvas.drawRect(0, 0, getRight(), getBottom(), dimPaint);
    }
  }

// ======== SCROLL ======== //

  private boolean canScroll() {
    return page == null || getPage().isCollapsed();
  }

  @Override
  public void scrollToPosition(int position) {
    if (!canScroll()) {
      return;
    }
    super.scrollToPosition(position);
  }

  @Override
  public void smoothScrollToPosition(int position) {
    if (!canScroll()) {
      return;
    }
    super.smoothScrollToPosition(position);
  }

  @Override
  public void smoothScrollBy(int dx, int dy) {
    if (!canScroll()) {
      return;
    }
    super.smoothScrollBy(dx, dy);
  }

  @Override
  public void scrollTo(int x, int y) {
    if (!canScroll()) {
      return;
    }
    super.scrollTo(x, y);
  }

  @Override
  public void scrollBy(int x, int y) {
    if (!canScroll()) {
      return;
    }
    super.scrollBy(x, y);
  }

  public ExpandablePageLayout getPage() {
    return page;
  }

  /**
   * @return Details of the currently expanded item. Returns an empty ExpandInfo object
   * if all items are collapsed.
   */
  public ExpandInfo getExpandInfo() {
    if (expandInfo == null) {
      expandInfo = ExpandInfo.createEmpty();
    }
    return expandInfo;
  }

  /**
   * Reduce overdraw by 1 level by removing the Activity Window's background
   * while the {@link ExpandablePageLayout} is open. No point in drawing it when
   * it's not visible to the user. This way, there's no extra overdraw while the
   * expandable page is open.
   */
  public void optimizeActivityBackgroundOverdraw(Activity activity) {
    activityWindow = activity.getWindow();
    activityWindowOrigBackground = activityWindow.getDecorView().getBackground();
  }

  public static int getAnimationStartDelay() {
    return 0;
  }

  /**
   * Contains details of the currently expanded item.
   */
  public static class ExpandInfo implements Parcelable {

    // Position of the currently expanded item.
    public int expandedItemPosition;

    // Adapter ID of the currently expanded item.
    public long expandedItemId;

    // Original location of the currently expanded item (that is, when the user selected this item).
    // Can be used for restoring states after collapsing.
    Rect expandedItemLocationRect;

    public ExpandInfo(int expandedItemPosition, long expandedItemId, Rect expandedItemLocationRect) {
      this.expandedItemPosition = expandedItemPosition;
      this.expandedItemId = expandedItemId;
      this.expandedItemLocationRect = expandedItemLocationRect;
    }

    static ExpandInfo createEmpty() {
      return new ExpandInfo(-1, -1, new Rect(0, 0, 0, 0));
    }

    boolean isEmpty() {
      return expandedItemPosition == -1 || expandedItemId == -1 || expandedItemLocationRect.height() == 0;
    }

    @Override
    public String toString() {
      return "ExpandInfo{" +
          "expandedItemPosition=" + expandedItemPosition +
          ", expandedItem Height=" + expandedItemLocationRect.height() +
          '}';
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeInt(this.expandedItemPosition);
      dest.writeLong(this.expandedItemId);
      dest.writeParcelable(this.expandedItemLocationRect, 0);
    }

    protected ExpandInfo(Parcel in) {
      this.expandedItemPosition = in.readInt();
      this.expandedItemId = in.readLong();
      this.expandedItemLocationRect = in.readParcelable(Rect.class.getClassLoader());
    }

    public static final Creator<ExpandInfo> CREATOR = new Creator<ExpandInfo>() {
      public ExpandInfo createFromParcel(Parcel source) {
        return new ExpandInfo(source);
      }

      public ExpandInfo[] newArray(int size) {
        return new ExpandInfo[size];
      }
    };
  }
}
