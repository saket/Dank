package me.saket.dank.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.support.annotation.Px;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

import me.saket.dank.R;

/**
 * A scrollable sheet that can wrap a RecyclerView and scroll together (not in parallel) in a nested manner.
 * This sheet consumes all scrolls made on a RecyclerView if it can scroll/fling any further in the direction
 * of the scroll.
 */
public class ScrollingRecyclerViewSheet extends FrameLayout implements NestedScrollingParent {

  private final Scroller flingScroller;
  private final List<SheetScrollChangeListener> scrollChangeListeners;
  private final int minimumFlingVelocity;
  private final int maximumFlingVelocity;

  private SheetStateChangeListener stateChangeListener;
  private RecyclerView childRecyclerView;
  private State currentState;
  private ValueAnimator scrollAnimator;
  private int peekHeight;
  private boolean scrollingEnabled;

  public enum State {
    EXPANDED,
    DRAGGING,
    AT_PEEK_HEIGHT,
  }

  public interface SheetStateChangeListener {

    void onStateChange(State newState);
  }

  public interface SheetScrollChangeListener {

    void onScrollChange(float newScrollY);
  }

  public ScrollingRecyclerViewSheet(Context context, AttributeSet attrs) {
    super(context, attrs);
    flingScroller = new Scroller(context);
    minimumFlingVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();
    maximumFlingVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();

    scrollChangeListeners = new ArrayList<>(3);

    if (hasSheetReachedTheTop()) {
      currentState = State.EXPANDED;
    }

    TypedArray properties = context.obtainStyledAttributes(attrs, R.styleable.ScrollingRecyclerViewSheet);
    setPeekHeight(properties.getDimensionPixelSize(R.styleable.ScrollingRecyclerViewSheet_peekHeight, 0));
    properties.recycle();

    setScrollingEnabled(true);
  }

  public void setOnStateChangeListener(SheetStateChangeListener listener) {
    stateChangeListener = listener;
    listener.onStateChange(currentState);
  }

  /**
   * "onSheet..." because {@link #setOnScrollChangeListener(OnScrollChangeListener)} is already a thing.
   */
  public void addOnSheetScrollChangeListener(SheetScrollChangeListener listener) {
    scrollChangeListeners.add(listener);
  }

  /**
   * True if either the sheet is fully expanded and cannot scroll up any further.
   */
  public boolean isExpanded() {
    return currentState == State.EXPANDED;
  }

  /**
   * True if either the sheet is fully hidden or at its peek-height.
   */
  public boolean isAtPeekHeightState() {
    return currentState == State.AT_PEEK_HEIGHT;
  }

  /**
   * Whether the sheet (and the list within) can scroll up any further when pulled downwards.
   */
  public boolean canScrollDownwardsAnyFurther() {
    boolean canSheetScrollDownwards = currentTopY() != maxScrollY();
    boolean canListScrollDownwards = childRecyclerView.canScrollVertically(-1);

    if (scrollingEnabled) {
      return canSheetScrollDownwards || canListScrollDownwards;
    } else {
      return canListScrollDownwards;
    }
  }

  /**
   * Whether the sheet (and the list within) can scroll down any further when pulled upwards.
   */
  public boolean canScrollUpwardsAnyFurther() {
    return currentTopY() != 0 || childRecyclerView.canScrollVertically(1);
  }

  /**
   * Set the visible height of this sheet when it's fully collapsed and docked at the bottom.
   */
  public void setPeekHeight(int peekHeight) {
    this.peekHeight = peekHeight;
  }

  public int getPeekHeight() {
    return peekHeight;
  }

  public void scrollTo(@Px int y) {
    attemptToConsumeScrollY(currentTopY() - y);
  }

  @Override
  public void scrollTo(int x, int y) {
    scrollTo(y);
  }

  public void smoothScrollTo(@Px int y) {
    if (isSmoothScrollingOngoing()) {
      scrollAnimator.cancel();
    }

    if (currentTopY() == y) {
      // Already at the same location.
      return;
    }

    scrollAnimator = ValueAnimator.ofFloat(currentTopY(), y);
    scrollAnimator.setInterpolator(new FastOutSlowInInterpolator());
    scrollAnimator.addUpdateListener(animation -> attemptToConsumeScrollY(currentTopY() - ((Float) animation.getAnimatedValue())));
    scrollAnimator.start();
  }

  public void scrollTo(@Px int y, boolean smoothScroll) {
    if (smoothScroll) {
      smoothScrollTo(y);
    } else {
      scrollTo(y);
    }
  }

  public boolean isSmoothScrollingOngoing() {
    return scrollAnimator != null && scrollAnimator.isStarted();
  }

  public void setScrollingEnabled(boolean enabled) {
    scrollingEnabled = enabled;
  }

// ======== PUBLIC APIs END ======== //

  /**
   * Maximum this sheet can scroll before it reaches its peek height.
   */
  public int maxScrollY() {
    int masScrollY = getHeight() - peekHeight;
    if (masScrollY < 0) {
      return 0;
    } else {
      return masScrollY;
    }
  }

  @Override
  public void addView(View child, int index, ViewGroup.LayoutParams params) {
    if (getChildCount() != 0) {
      throw new AssertionError("Can only host one RecyclerView");
    }
    if (!(child instanceof RecyclerView)) {
      throw new AssertionError("Only RecyclerView is supported");
    }
    super.addView(child, index, params);

    childRecyclerView = (RecyclerView) child;
    childRecyclerView.addOnScrollListener(scrollListener);
    childRecyclerView.setOverScrollMode(OVER_SCROLL_NEVER);
  }

  private boolean hasSheetReachedTheTop() {
    return currentTopY() <= 0;
  }

  /**
   * True if the sheet is docked at the bottom and is only peeking.
   */
  private boolean isSheetAtPeekHeight() {
    return currentTopY() >= maxScrollY();
  }

  private float currentTopY() {
    return getTranslationY();
  }

// ======== NESTED SCROLLING ======== //

  private float attemptToConsumeScrollY(float dy) {
    boolean scrollingDownwards = dy > 0;
    if (scrollingDownwards) {
      if (!hasSheetReachedTheTop()) {
        float adjustedDy = dy;
        if (currentTopY() - dy < 0) {
          // Don't let the sheet go beyond its top bounds.
          adjustedDy = currentTopY();
        }

        adjustOffsetBy(adjustedDy);
        return adjustedDy;
      }

    } else {
      boolean canChildViewScrollDownwardsAnymore = childRecyclerView.canScrollVertically(-1);
      if (!isSheetAtPeekHeight() && !canChildViewScrollDownwardsAnymore) {
        float adjustedDy = dy;
        if (currentTopY() - dy > maxScrollY()) {
          // Don't let the sheet go beyond its bottom bounds.
          adjustedDy = currentTopY() - maxScrollY();
        }

        adjustOffsetBy(adjustedDy);
        return adjustedDy;
      }
    }

    return 0;
  }

  private void adjustOffsetBy(float dy) {
    setTranslationY(getTranslationY() - dy);

    // Send a callback if the state changed.
    State newState;
    if (!canScrollDownwardsAnyFurther()) {
      newState = State.AT_PEEK_HEIGHT;
    } else if (hasSheetReachedTheTop()) {
      newState = State.EXPANDED;
    } else {
      newState = State.DRAGGING;
    }

    if (newState != currentState) {
      currentState = newState;

      if (stateChangeListener != null) {
        stateChangeListener.onStateChange(newState);
      }
    }

    // Scroll callback.
    for (int i = 0; i < scrollChangeListeners.size(); i++) {
      scrollChangeListeners.get(i).onScrollChange(getTranslationY());
    }
  }

  @Override
  public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
    // Always accept nested scroll events from the child. The decision of whether
    // or not to actually scroll is calculated inside onNestedPreScroll().
    return scrollingEnabled;
  }

  @Override
  public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
    flingScroller.forceFinished(true);
    float consumedY = attemptToConsumeScrollY(dy);
    consumed[1] = (int) consumedY;
  }

  @Override
  public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
    flingScroller.forceFinished(true);

    float velocityYAbs = Math.abs(velocityY);
    if (scrollingEnabled && velocityYAbs > minimumFlingVelocity && velocityYAbs < maximumFlingVelocity) {
      // Start flinging!
      flingScroller.fling(0, 0, (int) velocityX, (int) velocityY, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);

      new Handler().post(new Runnable() {
        private float lastY = flingScroller.getStartY();

        @Override
        public void run() {
          boolean isFlingOngoing = flingScroller.computeScrollOffset();
          if (isFlingOngoing) {
            float dY = flingScroller.getCurrY() - lastY;
            lastY = flingScroller.getCurrY();
            float distanceConsumed = attemptToConsumeScrollY(dY);

            // As soon as we stop scrolling, transfer the fling to the recyclerView.
            // This is hacky, but it works.
            if (distanceConsumed == 0f && hasSheetReachedTheTop()) {
              float transferVelocity = flingScroller.getCurrVelocity();
              if (velocityY < 0) {
                transferVelocity *= -1;
              }
              childRecyclerView.fling(0, ((int) transferVelocity));

            } else {
              // There's still more distance to be covered in this fling. Keep scrolling!
              post(this);
            }
          }
        }
      });

      // Consume all flings on the recyclerView. We'll manually check if they can actually be
      // used to scroll this sheet any further in the fling direction. If not, the fling is
      // transferred back to the RecyclerView.
      return true;

    } else {
      return super.onNestedPreFling(target, velocityX, velocityY);
    }
  }

  private RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
      if (!flingScroller.isFinished()) {
        // A fling is ongoing in the RecyclerView. Listen for the scroll offset and transfer
        // the fling to NonSnappingBottomSheet as soon as the recyclerView reaches the top.
        boolean hasReachedTop = recyclerView.computeVerticalScrollOffset() == 0;
        if (hasReachedTop) {
          // For some reasons, the sheet starts scrolling at a much higher velocity when the
          // fling is transferred.
          float transferVelocity = flingScroller.getCurrVelocity() / 4;
          if (dy < 0) {
            transferVelocity *= -1;
          }
          onNestedPreFling(recyclerView, 0, transferVelocity);
        }
      }
    }
  };

}
