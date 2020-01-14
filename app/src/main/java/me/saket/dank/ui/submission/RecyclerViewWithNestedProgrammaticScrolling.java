package me.saket.dank.ui.submission;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;

import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChild2;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

/**
 * When a programmatic scroll happens in RecyclerView, its {@link RecyclerView.ViewFlinger} does not
 * call {@link #startNestedScroll(int, int)} with TYPE_NON_TOUCH before calling
 * {@link #dispatchNestedPreScroll(int, int, int[], int[], int)}, which results in the call getting ignored.
 * <p>
 * This class manually starts a nested scrolling to workaround that behavior.
 */
public class RecyclerViewWithNestedProgrammaticScrolling extends RecyclerView implements NestedScrollingChild2 {

  public RecyclerViewWithNestedProgrammaticScrolling(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void smoothScrollBy(int dx, int dy) {
    startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
    super.smoothScrollBy(dx, dy);
  }

  @Override
  public void smoothScrollBy(int dx, int dy, Interpolator interpolator) {
    startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
    super.smoothScrollBy(dx, dy, interpolator);
  }

  @Override
  public void smoothScrollToPosition(int position) {
    startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
    super.smoothScrollToPosition(position);
  }

  @Override
  public void scrollBy(int x, int y) {
    startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
    super.scrollBy(x, y);
  }

  @Override
  public void scrollTo(int x, int y) {
    startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
    super.scrollTo(x, y);
  }

  @Override
  public void scrollToPosition(int position) {
    startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
    super.scrollToPosition(position);
  }
}
