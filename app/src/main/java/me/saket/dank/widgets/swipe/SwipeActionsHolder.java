package me.saket.dank.widgets.swipe;

import android.support.annotation.CheckResult;
import android.support.v4.util.ObjectsCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds {@link SwipeAction} for one side of a Swipeable View.
 */
public class SwipeActionsHolder {

  private final List<SwipeAction> actions;

  public SwipeActionsHolder(List<SwipeAction> actions) {
    this.actions = actions;
  }

  public static Builder builder() {
    return new Builder();
  }

  public List<SwipeAction> actions() {
    return actions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SwipeActionsHolder that = (SwipeActionsHolder) o;
    return ObjectsCompat.equals(actions, that.actions);
  }

  @Override
  public int hashCode() {
    return ObjectsCompat.hash(actions);
  }

  /**
   * @param swipeDirection Distance swiped relative to this layout. Always positive.
   */
  @CheckResult
  protected SwipeAction findActionAtSwipeDistance(int swipeableLayoutWidth, float swipeDistance, SwipeDirection swipeDirection) {
    if (swipeDistance > swipeableLayoutWidth) {
      throw new IllegalArgumentException("Swipe distance can't be bigger than the width of the swipeable layout: " +
          "swipeableLayoutWidth: " + swipeableLayoutWidth + ", swipeDistance: " + swipeDistance);
    }

    float totalWeights = calculateTotalWeights();

    switch (swipeDirection) {
      case END_TO_START:
        // Avoiding iterators intentionally because this method gets called on every motion event.
        float distanceAddedFromRight = 0;
        for (int i = 0; i < actions.size(); i++) {
          float actionReleaseLengthFromRight = (actions.get(i).layoutWeight() / totalWeights) * swipeableLayoutWidth;
          if (swipeDistance <= actionReleaseLengthFromRight + distanceAddedFromRight) {
            return actions.get(i);
          }
          distanceAddedFromRight += actionReleaseLengthFromRight;
        }
        throw new IllegalStateException("Couldn't find swipe action. actions: " + actions + ", swipeableLayoutWidth: "
            + swipeableLayoutWidth + ", swipeDistance: " + swipeDistance);

      case START_TO_END:
        float distanceAddedFromLeft = 0;
        for (int i = 0; i < actions.size(); i++) {
          float actionReleaseLengthFromLeft = (actions.get(i).layoutWeight() / totalWeights) * swipeableLayoutWidth;
          if (swipeDistance <= actionReleaseLengthFromLeft + distanceAddedFromLeft) {
            return actions.get(i);
          }
          distanceAddedFromLeft += actionReleaseLengthFromLeft;
        }
        throw new IllegalStateException("Couldn't find swipe action. actions: " + actions + ", swipeableLayoutWidth: "
            + swipeableLayoutWidth + ", swipeDistance: " + swipeDistance);

      default:
        throw new UnsupportedOperationException("Unknown swipe direction: " + swipeDirection);
    }
  }

  public boolean hasActions() {
    return !actions.isEmpty();
  }

  // TODO: avoid creating a new iterator because this method gets called on every motion event.
  private float calculateTotalWeights() {
    float totalWeights = 0;
    for (SwipeAction target : actions) {
      totalWeights += target.layoutWeight();
    }
    return totalWeights;
  }

  public static class Builder {

    private List<SwipeAction> actions = new ArrayList<>(4);

    public Builder add(SwipeAction action) {
      actions.add(action);
      return this;
    }

    public SwipeActionsHolder build() {
      return new SwipeActionsHolder(actions);
    }
  }
}
