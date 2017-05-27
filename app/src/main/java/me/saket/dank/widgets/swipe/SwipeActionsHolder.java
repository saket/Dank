package me.saket.dank.widgets.swipe;

import android.support.annotation.CheckResult;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Holds {@link SwipeAction} for one side of a Swipeable View.
 */
public class SwipeActionsHolder {

  private List<SwipeAction> actions = new ArrayList<>(4);

  public SwipeActionsHolder(List<SwipeAction> actions) {
    this.actions = actions;
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * @param swipeDirection Distance swiped relative to this layout.
   */
  @CheckResult
  protected SwipeAction findActionAtSwipeDistance(int swipeableLayoutWidth, float swipeDistance, SwipeDirection swipeDirection) {
    switch (swipeDirection) {
      case END_TO_START:
        for (int i = actions.size() - 1; i >= 0; i--) {
          if (swipeableLayoutWidth - swipeDistance >= calculateLeft(actions.get(i), swipeableLayoutWidth)) {
            return actions.get(i);
          }
        }
        throw new IllegalStateException("What? We should have found someone :O");

      case START_TO_END:
        for (int i = 0; i < actions.size(); i++) {
          if (swipeDistance <= calculateRight(actions.get(i), swipeableLayoutWidth)) {
            return actions.get(i);
          }
        }

        Timber.w("Error:");
        for (int i = 0; i < actions.size(); i++) {
          int right = calculateRight(actions.get(i), swipeableLayoutWidth);
          Timber.i("Right of: %s: %s", actions.get(i).name(), right);
          if (swipeDistance <= right) {
            return actions.get(i);
          }
        }
        throw new IllegalStateException("What? We should have found someone :O swipeDistance: " + swipeDistance);

      default:
        throw new UnsupportedOperationException("Unknown swipe direction: " + swipeDirection);
    }
  }

  public boolean contains(SwipeAction swipeAction) {
    return actions.contains(swipeAction);
  }

  /**
   * Calculate left position of <var>releaseTarget</var> relative to this ViewGroup.
   * We calculate the positions manually because we're not adding them as child Views.
   */
  private int calculateLeft(SwipeAction releaseTarget, int swipeableLayoutWidth) {
    float totalWeights = calculateTotalWeights();

    int distanceAddedFromLeft = 0;

    for (SwipeAction target : actions) {
      if (target == releaseTarget) {
        return distanceAddedFromLeft;
      }
      float targetWeight = releaseTarget.layoutWeight() / totalWeights;
      int targetImaginaryWidth = (int) (swipeableLayoutWidth * targetWeight);
      distanceAddedFromLeft += targetImaginaryWidth;
    }

    throw new IllegalStateException("What? We should have found something :O");
  }

  /**
   * Calculate right position of <var>releaseTarget</var> relative to this ViewGroup.
   */
  private int calculateRight(SwipeAction releaseTarget, int swipeableLayoutWidth) {
    float totalWeights = calculateTotalWeights();

    int distanceAddedFromLeft = 0;

    for (SwipeAction target : actions) {
      float targetWeight = releaseTarget.layoutWeight() / totalWeights;
      int targetImaginaryWidth = (int) (swipeableLayoutWidth * targetWeight);

      if (target == releaseTarget) {
        return distanceAddedFromLeft + targetImaginaryWidth;
      }

      distanceAddedFromLeft += targetImaginaryWidth;
    }

    throw new IllegalStateException("What? We should have found something :O");
  }

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
