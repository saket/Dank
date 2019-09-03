package me.saket.dank.widgets;

import android.content.Context;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;

import androidx.recyclerview.widget.RecyclerView;

import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.BaseExpandablePageLayout;

public class ToolbarExpandableSheet extends BaseExpandablePageLayout {

  private StateChangeListener stateChangeListener;
  private State currentState;
  private float elevationOnExpand;

  public enum State {
    COLLAPSING,
    COLLAPSED,
    EXPANDING,
    EXPANDED
  }

  public interface StateChangeListener {
    void onStateChange(State newState);
  }

  public ToolbarExpandableSheet(Context context, AttributeSet attrs) {
    super(context, attrs);
    ButterKnife.bind(this, this);
    elevationOnExpand = context.getResources().getDimensionPixelSize(R.dimen.subreddit_toolbar_sheet_elevation) - getElevation();

    // Hide on start.
    currentState = State.COLLAPSED;
    Views.executeOnMeasure(this, () -> setClippedDimensions(getWidth(), 0));

    // Avoid the shadows from showing up above the sheet. This is done by passing in the
    // center location of this sheet as the top-location for the shadow, essentially hiding
    // it behind this sheet.
    setOutlineProvider(new ViewOutlineProvider() {
      @Override
      public void getOutline(View view, Outline outline) {
        outline.setRect(0, getClippedRect().height() / 2, getClippedRect().width(), getClippedRect().height());
      }
    });
  }

  public boolean isExpandedOrExpanding() {
    return currentState == State.EXPANDED || currentState == State.EXPANDING;
  }

  public boolean isCollapsed() {
    return currentState == State.COLLAPSED;
  }

  public void expand() {
    if (currentState == State.EXPANDED || currentState == State.EXPANDING) {
      return;
    }

    animateDimensions(getWidth(), getHeight());
    animate()
        .setDuration(getAnimationDurationMillis())
        .setInterpolator(getAnimationInterpolator())
        .withStartAction(() -> dispatchStateChangeCallback(State.EXPANDING))
        .withEndAction(() -> {
          dispatchStateChangeCallback(State.EXPANDED);

          animate()
              .translationZ(elevationOnExpand)
              .setDuration(150)
              .setInterpolator(new DecelerateInterpolator())
              .start();
        })
        .start();
  }

  public void collapse() {
    if (currentState == State.COLLAPSED || currentState == State.COLLAPSING) {
      return;
    }

    setTranslationZ(0f);

    animateDimensions(getWidth(), 0);
    animate()
        .setDuration(getAnimationDurationMillis())
        .setInterpolator(getAnimationInterpolator())
        .withStartAction(() -> dispatchStateChangeCallback(State.COLLAPSING))
        .withEndAction(() -> dispatchStateChangeCallback(State.COLLAPSED))
        .start();
  }

  public void hideOnOutsideClick(RecyclerView recyclerView) {
    recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
      @Override
      public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        if (isExpandedOrExpanding()) {
          collapse();
          return true;
        }
        return super.onInterceptTouchEvent(rv, e);
      }
    });
  }

  public void setStateChangeListener(StateChangeListener listener) {
    stateChangeListener = listener;
  }

  public long getCollapseAnimationDuration() {
    return getAnimationDurationMillis();
  }

// ======== PUBLIC APIs END ======== //

  private void dispatchStateChangeCallback(State state) {
    currentState = state;
    stateChangeListener.onStateChange(state);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    boolean touchLiesInsideVisibleSheet = getClippedRect().contains((int) ev.getX(), (int) ev.getY());
    boolean handledBySuper = super.dispatchTouchEvent(ev);
    return touchLiesInsideVisibleSheet || handledBySuper;
  }
}
