package me.saket.dank.widgets.swipe;

import android.content.Context;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class SwipeableLayout extends FrameLayout {

  private SwipeReleaseTargetsLayout startSwipeReleaseTargetsLayout;
  private SwipeReleaseTargetsLayout endSwipeReleaseTargetsLayout;
  private View swipeableChild;

  public SwipeableLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void addView(View child, int index, ViewGroup.LayoutParams params) {
    switch (index) {
      case 0:
      case 1:
        if (!(child instanceof SwipeReleaseTargetsLayout)) {
          throw new AssertionError("SwipeReleaseTargetsLayouts must be present at the bottom of the z-hierarchy");
        }

        int childLayoutGravity = ((FrameLayout.LayoutParams) child.getLayoutParams()).gravity;
        if (childLayoutGravity == Gravity.START) {
          startSwipeReleaseTargetsLayout = (SwipeReleaseTargetsLayout) child;
        } else if (childLayoutGravity == Gravity.END) {
          endSwipeReleaseTargetsLayout = (SwipeReleaseTargetsLayout) child;
        } else {
          throw new UnsupportedOperationException("Unsupported gravity. Can only use start/left or end/right");
        }
        break;

      case 2:
        swipeableChild = child;
        super.addView(child, index, params);
        break;

      default:
        throw new UnsupportedOperationException("SwipeableLayout supports 3 children: 2 SwipeReleaseTargetsLayouts and a \"swipeable\" layout");
    }
  }

  public void setSwipeTranslation(@FloatRange(from = -1f, to = 1f) float translationXFactor) {
    if (isLaidOut()) {
      throw new IllegalStateException("SwipeableLayout hasn't been measured yet!");
    }

    float swipeDistance = getWidth() * translationXFactor;
    swipeableChild.setTranslationX(swipeDistance);

    // TODO: Draw background color
    // TODO: Ask for icon

    if (swipeDistance == 0f) {
      

    } else {
      SwipeReleaseTarget releaseTarget = translationXFactor < 0f
          ? endSwipeReleaseTargetsLayout.releaseTargetAtSwipeDistance(Math.abs(swipeDistance), SwipeDirection.END_TO_START)
          : startSwipeReleaseTargetsLayout.releaseTargetAtSwipeDistance(Math.abs(swipeDistance), SwipeDirection.START_TO_END);
    }
  }


}
