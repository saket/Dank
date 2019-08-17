package me.saket.dank.widgets.swipe;

import static java.lang.Math.abs;
import static me.saket.dank.utils.Math2.clamp;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import me.saket.dank.utils.SimpleRecyclerViewOnChildAttachStateChangeListener;

/**
 * Not using {@link ItemTouchHelper}, because it only supports "dismissing" items with swipe.
 * There's no concept of them snapping back.
 */
public class RecyclerSwipeListener extends RecyclerView.SimpleOnItemTouchListener {

  private final GestureDetector gestureDetector;
  private boolean isSwiping;
  private SwipeableLayout viewBeingSwiped;

  public RecyclerSwipeListener(RecyclerView recyclerView) {
    gestureDetector = createSwipeGestureDetector(recyclerView);

    // Reset animation if the View gets detached for recycling.
    recyclerView.addOnChildAttachStateChangeListener(new SimpleRecyclerViewOnChildAttachStateChangeListener() {
      @Override
      public void onChildViewDetachedFromWindow(View view) {
        RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(view);
        if (viewHolder instanceof ViewHolderWithSwipeActions) {
          ((ViewHolderWithSwipeActions) viewHolder).getSwipeableLayout().setSwipeTranslation(0f);
        }
      }
    });
  }

  private GestureDetector createSwipeGestureDetector(RecyclerView recyclerView) {
    return new GestureDetector(recyclerView.getContext(), new GestureDetector.SimpleOnGestureListener() {
      private int[] viewLocationOnScreen = new int[2];
      private float viewTopOnSwipeStart;

      @Override
      public boolean onDown(MotionEvent e) {
        // RecyclerView#findChildViewUnder() requires touch coordinates relative to
        // itself. So rawX() and rawY() will not be correct.
        View childViewUnder = recyclerView.findChildViewUnder(e.getX(), e.getY());
        if (childViewUnder != null) {
          RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(childViewUnder);
          if (viewHolder instanceof ViewHolderWithSwipeActions) {
            viewBeingSwiped = ((ViewHolderWithSwipeActions) viewHolder).getSwipeableLayout();
          } else {
            viewBeingSwiped = null;
          }
        } else {
          viewBeingSwiped = null;
        }

        // Clearing everything here because ACTION_UP doesn't get called if no touch event was intercepted.
        isSwiping = false;
        if (viewBeingSwiped != null) {
          // In SubmissionFragment, the comment list is contained in another scrolling layout.
          // So viewBeingSwiped.getTop() cannot be used as it'll be relative to its parent.
          viewBeingSwiped.getLocationOnScreen(viewLocationOnScreen);
          viewTopOnSwipeStart = viewLocationOnScreen[1];
        } else {
          viewTopOnSwipeStart = -1;
        }
        return false;
      }

      @Override
      public boolean onScroll(MotionEvent fromEvent, MotionEvent toEvent, float distanceX, float distanceY) {
        if (viewBeingSwiped == null) {
          return false;
        }

        viewBeingSwiped.getLocationOnScreen(viewLocationOnScreen);
        if (viewTopOnSwipeStart != viewLocationOnScreen[1]) {
          // List is being scrolled.
          return false;
        }

        // The swipe should start horizontally, but we'll let the gesture continue in any direction after that.
        boolean isHorizontalSwipe = abs(distanceX) > abs(distanceY) * 2;
        isSwiping = !viewBeingSwiped.isSettlingBackToPosition() && (viewBeingSwiped.hasCrossedSwipeDistanceThreshold() || isHorizontalSwipe);

        if (!isSwiping) {
          return false;
        }

        // Add inertia if swiping isn't enabled.
        float swipeTranslation = viewBeingSwiped.getSwipeTranslation();
        boolean swipingFromEndToStart = swipeTranslation < 0f;
        boolean swipingBackToPosition = Math.signum(swipeTranslation) == Math.signum(distanceX);
        SwipeDirection swipeDirection = swipingFromEndToStart ? SwipeDirection.END_TO_START : SwipeDirection.START_TO_END;
        if (!swipingBackToPosition && !viewBeingSwiped.isSwipeEnabled(swipeDirection)) {
          distanceX /= 10;
        }

        float newTranslationX = swipeTranslation - distanceX;
        float newTranslationXWithinBounds = clamp(newTranslationX, -viewBeingSwiped.getWidth(), viewBeingSwiped.getWidth());
        viewBeingSwiped.setSwipeTranslation(newTranslationXWithinBounds);
        return true;
      }
    });
  }

  @Override
  public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent event) {
    //return handleTouch(recyclerView, event);
    gestureDetector.onTouchEvent(event);
    if (isSwiping) {
      recyclerView.requestDisallowInterceptTouchEvent(true);
      return true;
    }

    //noinspection RedundantIfStatement
    if (viewBeingSwiped != null && viewBeingSwiped.isSettlingBackToPosition()) {
      return true;
    }

    return false;
  }

  @Override
  public void onTouchEvent(RecyclerView recyclerView, MotionEvent event) {
    //handleTouch(recyclerView, event);
    gestureDetector.onTouchEvent(event);

    if (isSwiping && event.getAction() == MotionEvent.ACTION_UP) {
      viewBeingSwiped.handleOnRelease();
    }

    if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
      viewBeingSwiped.animateBackToPosition();
    }
  }
}
