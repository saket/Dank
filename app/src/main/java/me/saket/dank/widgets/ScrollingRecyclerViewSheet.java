package me.saket.dank.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.core.view.NestedScrollingParent2;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import me.saket.dank.utils.Animations;

/**
 * A scrollable sheet that can wrap a RecyclerView and scroll together (not in parallel) in a nested manner.
 * This sheet consumes all scrolls made on a RecyclerView if it can scroll/fling any further in the direction
 * of the scroll.
 * <p>
 * It also acts as a fullscreen container for the RV in case the RV does not contain enough items to fill
 * the screen.
 */
public class ScrollingRecyclerViewSheet extends FrameLayout implements NestedScrollingParent2 {

  private final List<SheetScrollChangeListener> scrollChangeListeners;

  private RecyclerView childRecyclerView;
  private State currentState;
  private ValueAnimator scrollAnimator;
  private boolean scrollingEnabled;
  private int maxScrollY;

  public enum State {
    EXPANDED,
    DRAGGING,
    AT_MAX_SCROLL_Y,
  }

  public interface SheetScrollChangeListener {
    void onScrollChange(float newScrollY);
  }

  public ScrollingRecyclerViewSheet(Context context, AttributeSet attrs) {
    super(context, attrs);
    scrollChangeListeners = new ArrayList<>(3);

    if (hasSheetReachedTheTop()) {
      currentState = State.EXPANDED;
    }
    setScrollingEnabled(true);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    // Maintain scroll when keyboard shows up. In case (h < oldH) turns out to be too generic,
    // consider using (getBottom() + statusBarHeight < deviceDisplayHeight) for checking if
    // keyboard is visible.
    if (oldh != 0 && h != oldh) {
      smoothScrollTo(h - oldh);
    }
  }

  /**
   * "onSheet..." because {@link #setOnScrollChangeListener(OnScrollChangeListener)} is already a thing.
   */
  public void addOnSheetScrollChangeListener(SheetScrollChangeListener listener) {
    scrollChangeListeners.add(listener);
  }

  @CheckResult
  public Observable<Float> streamSheetScrollChanges() {
    return Observable.create(emitter -> {
      SheetScrollChangeListener listener = newScrollY -> emitter.onNext(newScrollY);
      post(() -> listener.onScrollChange(currentScrollY()));    // Initial value.
      addOnSheetScrollChangeListener(listener);
      emitter.setCancellable(() -> removeOnSheetScrollChangeListener(listener));
    });
  }

  public void removeOnSheetScrollChangeListener(SheetScrollChangeListener listener) {
    scrollChangeListeners.remove(listener);
  }

  /**
   * Whether the sheet (and the list within) can scroll up any further when pulled downwards.
   */
  public boolean canScrollDownwardsAnyFurther() {
    boolean canSheetScrollDownwards = currentScrollY() < maxScrollY;
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
    return currentScrollY() != 0 || childRecyclerView.canScrollVertically(1);
  }

  /**
   * Set the maximum Y this sheet can scroll to.
   */
  public void setMaxScrollY(int maxScrollY) {
    this.maxScrollY = maxScrollY;
  }

  public void scrollTo(@Px int y) {
    attemptToConsumeScrollY(currentScrollY() - y);
  }

  @Override
  public void scrollTo(int x, int y) {
    scrollTo(y);
  }

  public void smoothScrollTo(@Px int y) {
    if (isSmoothScrollingOngoing()) {
      scrollAnimator.cancel();
    }

    if (currentScrollY() == y) {
      // Already at the same location.
      return;
    }

    scrollAnimator = ValueAnimator.ofFloat(currentScrollY(), y);
    scrollAnimator.setInterpolator(Animations.INTERPOLATOR);
    scrollAnimator.addUpdateListener(animation -> attemptToConsumeScrollY(currentScrollY() - ((Float) animation.getAnimatedValue())));
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
    childRecyclerView.setOverScrollMode(OVER_SCROLL_NEVER);
  }

  public boolean hasSheetReachedTheTop() {
    return currentScrollY() <= 0;
  }

  /**
   * True if the sheet has reached its max scroll Y and cannot scroll further.
   */
  public boolean isAtMaxScrollY() {
    return currentScrollY() >= maxScrollY;
  }

  public float currentScrollY() {
    return getTranslationY();
  }

// ======== NESTED SCROLLING ======== //

  private float attemptToConsumeScrollY(float dy) {
    boolean listScrollingDownwards = dy > 0;
    if (listScrollingDownwards) {
      if (!hasSheetReachedTheTop()) {
        float adjustedDy = dy;
        if (currentScrollY() - dy < 0) {
          // Don't let the sheet go beyond its top bounds.
          adjustedDy = currentScrollY();
        }

        adjustOffsetBy(adjustedDy);
        return adjustedDy;
      }

    } else {
      boolean canChildViewScrollDownwardsAnymore = childRecyclerView.canScrollVertically(-1);
      if (!isAtMaxScrollY() && !canChildViewScrollDownwardsAnymore) {
        float adjustedDy = dy;
        if (currentScrollY() - dy > maxScrollY) {
          // Don't let the sheet go beyond its bottom bounds.
          adjustedDy = currentScrollY() - maxScrollY;
        }

        adjustOffsetBy(adjustedDy);
        return adjustedDy;
      }
    }

    return 0;
  }

  private void adjustOffsetBy(float dy) {
    float translationY = currentScrollY() - dy;
    setTranslationY(translationY);

    // Send a callback if the state changed.
    State newState;
    if (!canScrollDownwardsAnyFurther()) {
      newState = State.AT_MAX_SCROLL_Y;
    } else if (hasSheetReachedTheTop()) {
      newState = State.EXPANDED;
    } else {
      newState = State.DRAGGING;
    }

    if (newState != currentState) {
      currentState = newState;
    }

    // Scroll callback.
    for (int i = 0; i < scrollChangeListeners.size(); i++) {
      scrollChangeListeners.get(i).onScrollChange(currentScrollY());
    }
  }

  @Override
  public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
    return onStartNestedScroll(child, target, axes);
  }

  @Override
  public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type) {

  }

  @Override
  public void onStopNestedScroll(@NonNull View target, int type) {

  }

  @Override
  public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {

  }

  @Override
  public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
    onNestedPreScroll(target, dx, dy, consumed);
  }

  @Override
  public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
    // Always accept nested scroll events from the child. The decision of whether
    // or not to actually scroll is calculated inside onNestedPreScroll().
    return scrollingEnabled;
  }

  @Override
  public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
    float consumedY = attemptToConsumeScrollY(dy);
    consumed[1] = (int) consumedY;
  }
}
