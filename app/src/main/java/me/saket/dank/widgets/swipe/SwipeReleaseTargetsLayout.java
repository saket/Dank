package me.saket.dank.widgets.swipe;

import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for {@link SwipeReleaseTarget}.
 */
public class SwipeReleaseTargetsLayout extends LinearLayout {

  private List<SwipeReleaseTarget> releaseTargets = new ArrayList<>(4);

  public SwipeReleaseTargetsLayout(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void addView(View child, int index, ViewGroup.LayoutParams params) {
    //super.addView(child, index, params);

    if (!(child instanceof SwipeReleaseTarget)) {
      throw new IllegalStateException("Can only contain SwipeReleaseTargets Views");
    }
    releaseTargets.add(index, (SwipeReleaseTarget) child);
  }

  @Override
  public void removeView(View view) {
    //noinspection SuspiciousMethodCalls
    releaseTargets.remove(view);
    super.removeView(view);
  }

  /**
   * @param swipeDirection Distance swiped relative to this layout.
   */
  @CheckResult
  protected SwipeReleaseTarget releaseTargetAtSwipeDistance(float swipeDistance, SwipeDirection swipeDirection) {
    switch (swipeDirection) {
      case END_TO_START:
        for (int i = releaseTargets.size() - 1; i >= 0; i--) {
          if (getWidth() - swipeDistance > calculateLeft(releaseTargets.get(i))) {
            return releaseTargets.get(i);
          }
        }
        throw new IllegalStateException("What? We should have found someone :O");

      case START_TO_END:
        for (int i = 0; i < releaseTargets.size(); i++) {
          if (swipeDistance < calculateRight(releaseTargets.get(i))) {
            return releaseTargets.get(i);
          }
        }
        throw new IllegalStateException("What? We should have found someone :O");

      default:
        throw new UnsupportedOperationException("Unknown swipe direction: " + swipeDirection);
    }
  }

  /**
   * Calculate left position of <var>releaseTarget</var> relative to this ViewGroup.
   * We calculate the positions manually because we're not adding them as child Views.
   */
  private int calculateLeft(SwipeReleaseTarget releaseTarget) {
    float totalWeights = calculateTotalWeights();

    int distanceAddedFromLeft = 0;

    for (SwipeReleaseTarget target : releaseTargets) {
      if (target == releaseTarget) {
        return distanceAddedFromLeft;
      }
      float targetWeight = releaseTarget.layoutWeight() / totalWeights;
      int targetImaginaryWidth = (int) (getWidth() * targetWeight);
      distanceAddedFromLeft += targetImaginaryWidth;
    }

    throw new IllegalStateException("What? We should have found something :O");
  }

  /**
   * Calculate right position of <var>releaseTarget</var> relative to this ViewGroup.
   */
  private int calculateRight(SwipeReleaseTarget releaseTarget) {
    float totalWeights = calculateTotalWeights();

    int distanceAddedFromLeft = 0;

    for (SwipeReleaseTarget target : releaseTargets) {
      float targetWeight = releaseTarget.layoutWeight() / totalWeights;
      int targetImaginaryWidth = (int) (getWidth() * targetWeight);

      if (target == releaseTarget) {
        return distanceAddedFromLeft + targetImaginaryWidth;
      }

      distanceAddedFromLeft += targetImaginaryWidth;
    }

    throw new IllegalStateException("What? We should have found something :O");
  }

  private float calculateTotalWeights() {
    float totalWeights = 0;
    for (SwipeReleaseTarget target : releaseTargets) {
      totalWeights += target.layoutWeight();
    }
    return totalWeights;
  }
}
