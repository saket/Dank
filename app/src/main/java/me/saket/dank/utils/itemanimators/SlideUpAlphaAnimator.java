package me.saket.dank.utils.itemanimators;

import android.view.View;

import me.saket.dank.utils.Animations;

public class SlideUpAlphaAnimator extends SlideAlphaAnimator<SlideUpAlphaAnimator> {

  public static SlideUpAlphaAnimator create() {
    return new SlideUpAlphaAnimator()
        .withInterpolator(Animations.INTERPOLATOR)
        .withAddDuration(250)
        .withRemoveDuration(250);
  }

  public SlideUpAlphaAnimator() {
    super(0);
  }

  @Override
  protected float getAnimationTranslationX(View itemView) {
    return 0;
  }

  protected float getAnimationTranslationY(View itemView) {
    return dpToPx(32, itemView.getContext());
  }
}
