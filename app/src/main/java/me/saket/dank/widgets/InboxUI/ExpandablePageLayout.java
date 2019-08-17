package me.saket.dank.widgets.InboxUI;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.exceptions.Exceptions;
import me.saket.dank.utils.Views;

/**
 * An expandable / collapsible ViewGroup to be used with a {@link InboxRecyclerView}.
 */
public class ExpandablePageLayout extends BaseExpandablePageLayout implements PullToCollapseListener.OnPullListener {

  @Nullable private View activityToolbar;  // Toolbar inside the parent page, not in this page.
  @Nullable ExpandablePageLayout nestedPage;

  private static Method suppressLayoutMethod;

  private PullToCollapseListener pullToCollapseListener;
  private OnPullToCollapseIntercepter onPullToCollapseIntercepter;
  private List<StateChangeCallbacks> stateChangeCallbacks;
  private InternalPageCallbacks internalStateChangeCallbacksForNestedPage;
  private InternalPageCallbacks internalStateChangeCallbacksForInboxRecyclerView;

  private PageState currentPageState;
  private ValueAnimator toolbarAnimator;
  private float expandedAlpha;
  private float collapsedAlpha;
  private boolean isFullyCoveredByNestedPage;
  private boolean pullToCollapseEnabled;

  public enum PageState {
    COLLAPSING,
    COLLAPSED,
    EXPANDING,
    EXPANDED;

    public boolean isCollapsed() {
      return this == COLLAPSED;
    }
  }

  public interface OnPullToCollapseIntercepter {
    /**
     * Called when the user makes a vertical swipe gesture, which is registered as a pull-to-collapse gesture.
     * This is called once per gesture, when the user's finger touches this page. <code>ExpandablePage</code>
     * starts accepting this gesture if this method returns false.
     * <p>
     * So, if you have other vertically scrollable Views in your layout (RecyclerView, ListView, ScrollView, etc.),
     * you can return true to consume a gesture after verifying that the touch event lies on one of those Views.
     * This will block <code>ExpandablePage</code> from processing the gesture until the finger is lifted.
     *
     * @param downX          X-location from where the gesture started.
     * @param downY          Y-location from where the gesture started.
     * @param upwardPagePull True if the PAGE is being pulled upwards. Remember that upward swipe == downward
     *                       scroll and vice versa.
     * @return True to consume this touch event. False otherwise.
     */
    boolean onInterceptPullToCollapseGesture(MotionEvent event, float downX, float downY, boolean upwardPagePull);
  }

  /**
   * Implement this to receive state callbacks about an <code>ExpandingPage</code>. Use with
   * {@link ExpandablePageLayout#addStateChangeCallbacks(StateChangeCallbacks)}. If you use a Fragment
   * inside your <code>ExpandablePage</code>, it's best to make your Fragment implement this interface.
   */
  public interface StateChangeCallbacks {
    /**
     * Called when the user has selected an item and the <code>ExpandablePage</code> is going to be expand.
     */
    void onPageAboutToExpand(long expandAnimDuration);

    /**
     * Called when either the <code>ExpandablePage</code>'s expand animation is complete or if the
     * <code>ExpandablePage</code> was expanded immediately. At this time, the page is fully covering the list.
     */
    void onPageExpanded();

    /**
     * Called when the user has chosen to close the expanded item and the <code>ExpandablePage</code> is going to
     * be collapse.
     */
    void onPageAboutToCollapse(long collapseAnimDuration);

    /**
     * Called when the page's collapse animation is complete. At this time, it's totally invisible to the user.
     */
    void onPageCollapsed();
  }

  // Used internally, by InboxRecyclerView.
  interface InternalPageCallbacks {
    /**
     * Called when this page has fully covered the list. This can happen in two situations: when the page has
     * fully expanded and when the page has moved back to its position after being pulled.
     */
    void onPageFullyCovered();

    /**
     * Called when this page is going to be collapsed.
     */
    void onPageAboutToCollapse();

    /**
     * Called when this page has fully collapsed and is no longer visible.
     */
    void onPageFullyCollapsed();

    /**
     * Page is being pulled. Sync the scroll with the list.
     */
    void onPagePull(float deltaY);

    /**
     * Called when this page was released while being pulled.
     *
     * @param collapseEligible Whether the page was pulled enough for collapsing it.
     */
    void onPageRelease(boolean collapseEligible);
  }

  public ExpandablePageLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    expandedAlpha = 1f;
    collapsedAlpha = 0f;

    // Hidden on start
    setAlpha(expandedAlpha);
    setVisibility(View.INVISIBLE);
    changeState(PageState.COLLAPSED);

    // Handles pull-to-collapse-this-page gestures
    setPullToCollapseEnabled(true);
    pullToCollapseListener = new PullToCollapseListener(getContext(), this);
    pullToCollapseListener.addOnPullListener(this);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    // Cache before-hand.
    new Thread(() -> {
      if (suppressLayoutMethod == null) {
        setSuppressLayoutMethodUsingReflection(this, false);
      }
    }).start();
  }

  protected void setup(View parentActivityToolbar) {
    activityToolbar = parentActivityToolbar;

    // NOTE: if I'm still seeing a problem where the toolbar's height is reported
    // as way too high, use a lambda instead of evaluating the toolbar's height before-hand.
    activityToolbar.post(() -> setPullToCollapseDistanceThreshold(parentActivityToolbar.getHeight()));
  }

  /**
   * The distance after which the page can collapse when pulled.
   */
  public void setPullToCollapseDistanceThreshold(int threshold) {
    pullToCollapseListener.setCollapseDistanceThreshold(threshold);
  }

  public void setPullToCollapseEnabled(boolean enabled) {
    pullToCollapseEnabled = enabled;
  }

  protected void changeState(PageState newPageState) {
    currentPageState = newPageState;
  }

  @Override
  public boolean hasOverlappingRendering() {
    // According to Google developer videos, returning false improves performance when animating alpha.
    return false;
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    // Ignore touch events until the page is fully expanded for avoiding accidental taps.
    if (isExpanded()) {
      super.dispatchTouchEvent(ev);
    }

    // Consume all touch events to avoid them leaking behind.
    return true;
  }

// ======== PULL TO COLLAPSE ======== //

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    boolean intercepted = false;
    if (pullToCollapseEnabled && getVisibility() == VISIBLE) {
      intercepted = pullToCollapseListener.onTouch(this, event);
    }

    return intercepted || super.onInterceptTouchEvent(event);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return (pullToCollapseEnabled && pullToCollapseListener.onTouch(this, event)) || super.onTouchEvent(event);
  }

  @Override
  public void onPull(float deltaY, float currentTranslationY, boolean upwardPull, boolean deltaUpwardPull, boolean collapseEligible) {
    // In case the user pulled the page before it could fully open (and while the toolbar was still hiding).
    stopToolbarAnimation();

    // Reveal the toolbar if this page is being pulled down or hide it back if it's being released.
    if (activityToolbar != null) {
      updateToolbarTranslationY(currentTranslationY > 0f, currentTranslationY);
    }

    // Sync the positions of the list items with this page.
    dispatchOnPagePullCallbacks(deltaY);
  }

  @Override
  public void onRelease(boolean collapseEligible) {
    dispatchOnPageReleasedCallback(collapseEligible);

    // The list should either collapse or animate back its items out of the list.
    if (collapseEligible) {
      dispatchOnPageAboutToCollapseCallback();

    } else {
      if (isCollapsedOrCollapsing()) {
        // Let the page collapse in peace.
        return;
      }

      changeState(PageState.EXPANDED);
      stopAnyOngoingPageAnimation();

      // Restore everything to their expanded position.
      // 1. Hide Toolbar again
      if (activityToolbar != null) {
        animateToolbar(false, 0f);
      }

      // 2. Expand page again.
      if (getTranslationY() != 0f) {
        animate()
            .withLayer()
            .translationY(0f)
            .alpha(expandedAlpha)
            .setDuration(getAnimationDurationMillis())
            .setInterpolator(getAnimationInterpolator())
            .setListener(new AnimatorListenerAdapter() {
              boolean mCanceled;

              @Override
              public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mCanceled = false;
              }

              @Override
              public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                mCanceled = true;
              }

              @Override
              public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (mCanceled) {
                  return;
                }
                dispatchOnPageFullyCoveredCallback();
              }

            })
            .start();
      }
    }
  }

// ======== EXPAND / COLLAPSE ANIMATION ======== //

  /**
   * Expands this page (with animation) so that it fills the whole screen.
   */
  void expand(InboxRecyclerView.ExpandInfo expandInfo) {
    // Skip animations if Android hasn't measured Views yet.
    if (!isLaidOut() && getVisibility() != GONE) {
      throw new IllegalAccessError("Width / Height not available to expand");
    }

    // Ignore if already expanded.
    if (isExpandedOrExpanding()) {
      return;
    }

    // Place the expandable page on top of the expanding item.
    alignPageWithExpandingItem(expandInfo);

    // Animate!
    animatePageExpandCollapse(true, getWidth(), getHeight(), expandInfo);

    // Callbacks, just before the animation starts.
    dispatchOnAboutToExpandCallback();
  }

  /**
   * Expands this page instantly, without any animation. Use this when the user wants to directly
   * navigate to this page, by-passing the list.
   * <p>
   * It's allowed to call this directly without a RecyclerView in cases where there is no list. Like: DankPullCollapsibleActivity.
   */
  public void expandImmediately() {
    // Ignore if already expanded.
    if (currentPageState == PageState.EXPANDING || currentPageState == PageState.EXPANDED) {
      return;
    }

    setVisibility(VISIBLE);
    setAlpha(expandedAlpha);

    // Hide the toolbar as soon as we have its height (as expandImmediately()
    // could have been called before the Views were drawn).
    if (activityToolbar != null) {
      Views.executeOnMeasure(activityToolbar, () -> updateToolbarTranslationY(false, 0));
    }

    Views.executeOnMeasure(this, () -> {
      // Cover the whole screen right away. Don't need any animations.
      alignPageToCoverScreen();

      // Let the list know about this so that it dims itself
      // right away and does not draw any child Views
      dispatchOnAboutToExpandCallback();
      dispatchOnFullyExpandedCallback();
    });
  }

  /**
   * Collapses this page, back to its original state.
   */
  void collapse(InboxRecyclerView.ExpandInfo expandInfo) {
    // Ignore if already collapsed.
    if (currentPageState == PageState.COLLAPSED || currentPageState == PageState.COLLAPSING) {
      return;
    }

    // Fire!
    int targetWidth = expandInfo.expandedItemLocationRect.width();
    int targetHeight = expandInfo.expandedItemLocationRect.height();
    if (targetWidth == 0) {
      // Page must have expanded immediately after a state restoration.
      targetWidth = getWidth();
    }
    animatePageExpandCollapse(false, targetWidth, targetHeight, expandInfo);

    // Send state callbacks that the city is going to collapse.
    dispatchOnPageAboutToCollapseCallback();
  }

  /**
   * Places the expandable page exactly on top of the expanding list item
   * (including matching its height with the list item)
   */
  protected void alignPageWithExpandingItem(InboxRecyclerView.ExpandInfo expandInfo) {
    // Match height and location.
    setClippedDimensions(
        expandInfo.expandedItemLocationRect.width(),
        expandInfo.expandedItemLocationRect.height()
    );
    setTranslationY(expandInfo.expandedItemLocationRect.top);
  }

  protected void alignPageToCoverScreen() {
    resetClipping();
    setTranslationY(0f);
  }

  protected void animatePageExpandCollapse(boolean expand, int targetWidth, int targetHeight, InboxRecyclerView.ExpandInfo expandInfo) {
    float targetPageTranslationY = expand ? 0f : expandInfo.expandedItemLocationRect.top;
    final float targetPageTranslationX = expand ? 0f : expandInfo.expandedItemLocationRect.left;

    // If there's no record about the expanded list item (from whose place this page was expanded),
    // collapse just below the toolbar and not the window top to avoid closing the toolbar upon hiding.
    if (!expand && expandInfo.expandedItemLocationRect.height() == 0) {
      int toolbarBottom = activityToolbar != null ? activityToolbar.getBottom() : 0;
      targetPageTranslationY = Math.max(targetPageTranslationY, toolbarBottom);
    }

    setSuppressLayoutMethodUsingReflection(ExpandablePageLayout.this, true);

    if (expand) {
      setVisibility(View.VISIBLE);
    }

    setAlpha(expand ? collapsedAlpha : expandedAlpha);
    stopAnyOngoingPageAnimation();
    animate()
        .withLayer()
        .alpha(expand ? expandedAlpha : collapsedAlpha)
        .translationY(targetPageTranslationY)
        .translationX(targetPageTranslationX)
        .setDuration(getAnimationDurationMillis())
        .setInterpolator(getAnimationInterpolator())
        .setListener(new AnimatorListenerAdapter() {
          private boolean canceled;

          @Override
          public void onAnimationStart(Animator animation) {
            canceled = false;
          }

          @Override
          public void onAnimationCancel(Animator animation) {
            canceled = true;
          }

          @Override
          public void onAnimationEnd(Animator animation) {
            setSuppressLayoutMethodUsingReflection(ExpandablePageLayout.this, false);

            if (!canceled) {
              if (!expand) {
                setVisibility(View.INVISIBLE);
                dispatchOnPageCollapsedCallback();
              } else {
                dispatchOnFullyExpandedCallback();
              }
            }
          }
        })
        .setStartDelay(InboxRecyclerView.getAnimationStartDelay())
        .start();

    // Show the toolbar fully even if the page is going to collapse behind it
    float targetPageTranslationYForToolbar = targetPageTranslationY;
    if (!expand && activityToolbar != null && targetPageTranslationYForToolbar < activityToolbar.getBottom()) {
      targetPageTranslationYForToolbar = activityToolbar.getBottom();
    }

    if (activityToolbar != null) {
      // Hide / show the toolbar by pushing it up during expand and pulling it down during collapse.
      animateToolbar(
          !expand,    // When expand is false, !expand shows the toolbar.
          targetPageTranslationYForToolbar
      );
    }

    // Width & Height.
    animateDimensions(targetWidth, targetHeight);
  }

  private void animateToolbar(boolean show, float targetPageTranslationY) {
    if (getTranslationY() == targetPageTranslationY) {
      return;
    }

    final float toolbarCurrentBottom = activityToolbar != null ? activityToolbar.getBottom() + activityToolbar.getTranslationY() : 0;
    final float fromTy = Math.max(toolbarCurrentBottom, getTranslationY());

    // The hide animation happens a bit too quickly if the page has to travel a large
    // distance (when using the current interpolator: EASE). Let's try slowing it down.
    long speedFactor = 1L;
    if (show && Math.abs(targetPageTranslationY - fromTy) > getClippedHeight() * 2 / 5) {
      speedFactor *= 2L;
    }

    stopToolbarAnimation();

    // If the page lies behind the toolbar, use toolbar's current bottom position instead
    toolbarAnimator = ObjectAnimator.ofFloat(fromTy, targetPageTranslationY);
    toolbarAnimator.addUpdateListener(animation -> updateToolbarTranslationY(show, (float) animation.getAnimatedValue()));
    toolbarAnimator.setDuration(getAnimationDurationMillis() * speedFactor);
    toolbarAnimator.setInterpolator(getAnimationInterpolator());
    toolbarAnimator.setStartDelay(InboxRecyclerView.getAnimationStartDelay());
    toolbarAnimator.start();
  }

  /**
   * Helper method for showing / hiding the toolbar depending upon this page's current translationY.
   */
  private void updateToolbarTranslationY(boolean show, float pageTranslationY) {
    //noinspection ConstantConditions
    final int toolbarHeight = activityToolbar.getBottom();
    float targetTranslationY = pageTranslationY - toolbarHeight;

    if (show) {
      if (targetTranslationY > toolbarHeight) {
        targetTranslationY = toolbarHeight;
      }
      if (targetTranslationY > 0) {
        targetTranslationY = 0;
      }

    } else if (pageTranslationY >= toolbarHeight || activityToolbar.getTranslationY() <= -toolbarHeight) {
      // Hide.
      return;
    }

    activityToolbar.setTranslationY(targetTranslationY);
  }

  void stopAnyOngoingPageAnimation() {
    animate().cancel();
    stopToolbarAnimation();
  }

  private void stopToolbarAnimation() {
    if (toolbarAnimator != null) {
      toolbarAnimator.cancel();
    }
  }

// ======== OPTIMIZATIONS ======== //

  /**
   * To be used when another ExpandablePageLayout is shown inside this page.Z
   * This page will avoid all draw calls while the nested page is open to
   * minimize overdraw.
   * <p>
   * WARNING: DO NOT USE THIS IF THE NESTED PAGE IS THE ONLY PULL-COLLAPSIBLE
   * PAGE IN AN ACTIVITY.
   */
  public void setNestedExpandablePage(ExpandablePageLayout nestedPage) {
    this.nestedPage = nestedPage;

    nestedPage.setInternalStateCallbacksForNestedPage(new InternalPageCallbacks() {
      @Override
      public void onPageAboutToCollapse() {
        onPageBackgroundVisible();
      }

      @Override
      public void onPageFullyCollapsed() {
      }

      @Override
      public void onPagePull(float deltaY) {
        onPageBackgroundVisible();
      }

      @Override
      public void onPageRelease(boolean collapseEligible) {
        if (collapseEligible) {
          onPageBackgroundVisible();
        }
      }

      @Override
      public void onPageFullyCovered() {
        final boolean invalidate = !isFullyCoveredByNestedPage;
        isFullyCoveredByNestedPage = true;   // Skips draw() until visible again to the
        if (invalidate) {
          postInvalidate();
        }
      }

      public void onPageBackgroundVisible() {
        final boolean invalidate = isFullyCoveredByNestedPage;
        isFullyCoveredByNestedPage = false;
        if (invalidate) {
          postInvalidate();
        }
      }
    });
  }

  @Override
  public void draw(Canvas canvas) {
    // Or if the page is collapsed.
    if (currentPageState == PageState.COLLAPSED) {
      return;
    }
    super.draw(canvas);
  }

  @Override
  protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
    // When this page is fully covered by a nested ExpandablePage, avoid drawing any other child Views.
    //noinspection SimplifiableIfStatement
    if (isFullyCoveredByNestedPage && !(child instanceof ExpandablePageLayout)) {
      return false;
    }
    return super.drawChild(canvas, child, drawingTime);
  }

// ======== CALLBACKS ======== //

  private void dispatchOnPagePullCallbacks(float deltaY) {
    if (internalStateChangeCallbacksForNestedPage != null) {
      internalStateChangeCallbacksForNestedPage.onPagePull(deltaY);
    }
    if (internalStateChangeCallbacksForInboxRecyclerView != null) {
      internalStateChangeCallbacksForInboxRecyclerView.onPagePull(deltaY);
    }
  }

  private void dispatchOnPageReleasedCallback(boolean collapseEligible) {
    if (internalStateChangeCallbacksForNestedPage != null) {
      internalStateChangeCallbacksForNestedPage.onPageRelease(collapseEligible);
    }
    if (internalStateChangeCallbacksForInboxRecyclerView != null) {
      internalStateChangeCallbacksForInboxRecyclerView.onPageRelease(collapseEligible);
    }
  }

  private void dispatchOnAboutToExpandCallback() {
    // The state change must happen after the subscribers have been
    // notified that the page is going to expand
    changeState(PageState.EXPANDING);

    if (stateChangeCallbacks != null) {
      // Reverse loop to let listeners remove themselves while in the loop.
      for (int i = stateChangeCallbacks.size() - 1; i >= 0; i--) {
        stateChangeCallbacks.get(i).onPageAboutToExpand(getAnimationDurationMillis());
      }
    }

    onPageAboutToExpand(getAnimationDurationMillis());
  }

  @SuppressWarnings("Convert2streamapi")
  private void dispatchOnFullyExpandedCallback() {
    changeState(PageState.EXPANDED);
    dispatchOnPageFullyCoveredCallback();

    if (stateChangeCallbacks != null) {
      // Reverse loop to let listeners remove themselves while in the loop.
      for (int i = stateChangeCallbacks.size() - 1; i >= 0; i--) {
        stateChangeCallbacks.get(i).onPageExpanded();
      }
    }

    onPageExpanded();
  }

  /**
   * There's a difference between the page fully expanding and fully covering the list.
   * When the page is fully expanded, it may or may not be covering the list. This is
   * usually when the user is pulling the page.
   */
  private void dispatchOnPageFullyCoveredCallback() {
    if (internalStateChangeCallbacksForNestedPage != null) {
      internalStateChangeCallbacksForNestedPage.onPageFullyCovered();
    }
    if (internalStateChangeCallbacksForInboxRecyclerView != null) {
      internalStateChangeCallbacksForInboxRecyclerView.onPageFullyCovered();
    }
  }

  private void dispatchOnPageAboutToCollapseCallback() {
    // The state change must happen after the subscribers have been notified that the page is going to collapse.
    changeState(PageState.COLLAPSING);

    if (internalStateChangeCallbacksForNestedPage != null) {
      internalStateChangeCallbacksForNestedPage.onPageAboutToCollapse();
    }
    if (internalStateChangeCallbacksForInboxRecyclerView != null) {
      internalStateChangeCallbacksForInboxRecyclerView.onPageAboutToCollapse();
    }

    if (stateChangeCallbacks != null) {
      // Reverse loop to let listeners remove themselves while in the loop.
      for (int i = stateChangeCallbacks.size() - 1; i >= 0; i--) {
        StateChangeCallbacks callback = stateChangeCallbacks.get(i);
        callback.onPageAboutToCollapse(getAnimationDurationMillis());
      }
    }

    onPageAboutToCollapse(getAnimationDurationMillis());
  }

  private void dispatchOnPageCollapsedCallback() {
    changeState(PageState.COLLAPSED);

    if (internalStateChangeCallbacksForNestedPage != null) {
      internalStateChangeCallbacksForNestedPage.onPageFullyCollapsed();
    }
    if (internalStateChangeCallbacksForInboxRecyclerView != null) {
      internalStateChangeCallbacksForInboxRecyclerView.onPageFullyCollapsed();
    }

    if (stateChangeCallbacks != null) {
      // Reverse loop to let listeners remove themselves while in the loop.
      for (int i = stateChangeCallbacks.size() - 1; i >= 0; i--) {
        StateChangeCallbacks callback = stateChangeCallbacks.get(i);
        callback.onPageCollapsed();
      }
    }
    onPageCollapsed();
  }

  protected void onPageExpanded() {
    // For rent.
  }

  protected void onPageAboutToExpand(long expandAnimDuration) {
    // For rent.
  }

  protected void onPageAboutToCollapse(long collapseAnimDuration) {
    // For rent.
  }

  /**
   * Page is totally invisible to the user when this is called.
   */
  protected void onPageCollapsed() {
    // For rent.
  }

  /**
   * Offer a pull-to-collapse to a listener if it wants to block it. If a nested page is registered
   * and the touch was made on it, block it right away.
   */
  @SuppressWarnings("SimplifiableIfStatement")
  boolean handleOnPullToCollapseIntercept(MotionEvent event, float downX, float downY, boolean deltaUpwardSwipe) {
    if (nestedPage != null && nestedPage.isExpandedOrExpanding() && nestedPage.getClippedRect().contains((int) downX, (int) downY)) {
      // Block this pull if it's being made inside a nested page. Let the nested page's pull-listener consume this event.
      // We should use nested scrolling in the future to make this smarter.
      // TODO: 20/03/17 Do we even need to call the nested page's listener?
      nestedPage.handleOnPullToCollapseIntercept(event, downX, downY, deltaUpwardSwipe);
      return true;

    } else if (onPullToCollapseIntercepter != null) {
      return onPullToCollapseIntercepter.onInterceptPullToCollapseGesture(event, downX, downY, deltaUpwardSwipe);

    } else {
      return false;
    }
  }

// ======== SETTERS & GETTERS ======== //

  /**
   * Calls for the associated InboxRecyclerView.
   */
  void setInternalStateCallbacksForList(InternalPageCallbacks listCallbacks) {
    internalStateChangeCallbacksForInboxRecyclerView = listCallbacks;
  }

  void setInternalStateCallbacksForNestedPage(InternalPageCallbacks nestedPageCallbacks) {
    internalStateChangeCallbacksForNestedPage = nestedPageCallbacks;
  }

  public boolean isExpanded() {
    return currentPageState == PageState.EXPANDED;
  }

  public boolean isExpandingOrCollapsing() {
    return currentPageState == PageState.EXPANDING || currentPageState == PageState.COLLAPSING;
  }

  public boolean isCollapsing() {
    return currentPageState == PageState.COLLAPSING;
  }

  public boolean isCollapsed() {
    return currentPageState == PageState.COLLAPSED;
  }

  public PageState getCurrentState() {
    return currentPageState;
  }

  public boolean isExpanding() {
    return currentPageState == PageState.EXPANDING;
  }

  public boolean isExpandedOrExpanding() {
    return currentPageState == PageState.EXPANDED || currentPageState == PageState.EXPANDING;
  }

  public boolean isCollapsedOrCollapsing() {
    return currentPageState == PageState.COLLAPSING || currentPageState == PageState.COLLAPSED;
  }

  public void addStateChangeCallbacks(StateChangeCallbacks callbacks) {
    if (this.stateChangeCallbacks == null) {
      this.stateChangeCallbacks = new ArrayList<>(4);
    }
    stateChangeCallbacks.add(callbacks);
  }

  public void removeStateChangeCallbacks(StateChangeCallbacks callbacks) {
    stateChangeCallbacks.remove(callbacks);
  }

  public void setPullToCollapseIntercepter(OnPullToCollapseIntercepter intercepter) {
    onPullToCollapseIntercepter = intercepter;
  }

  /**
   * Listener that gets called when this page is being pulled.
   */
  public void addOnPullListener(PullToCollapseListener.OnPullListener listener) {
    pullToCollapseListener.addOnPullListener(listener);
  }

  public void removeOnPullListener(PullToCollapseListener.OnPullListener pullListener) {
    pullToCollapseListener.removeOnPullListener(pullListener);
  }

  /**
   * Alpha of this page when it's collapsed.
   */
  public void setCollapsedAlpha(float collapsedAlpha) {
    this.collapsedAlpha = collapsedAlpha;
  }

// ======== REFLECTION ======== //

  private static void setSuppressLayoutMethodUsingReflection(ExpandablePageLayout layout, boolean suppress) {
    try {
      if (suppressLayoutMethod == null) {
        suppressLayoutMethod = ViewGroup.class.getMethod("suppressLayout", boolean.class);
      }
      suppressLayoutMethod.invoke(layout, suppress);

    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw Exceptions.propagate(e);
    }
  }
}
