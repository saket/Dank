package me.saket.dank.utils.itemanimators;

import android.view.View;

import me.saket.dank.utils.Animations;

public class SlideLeftAlphaAnimator extends SlideAlphaAnimator<SlideLeftAlphaAnimator> {

  @SuppressWarnings("deprecation")
  public static SlideLeftAlphaAnimator create() {
    return new SlideLeftAlphaAnimator(0)
        .withInterpolator(Animations.INTERPOLATOR)
        .withAddDuration(250)
        .withRemoveDuration(250);
  }

  /**
   * @deprecated Use {@link #create()} instead.
   */
  public SlideLeftAlphaAnimator(int itemViewElevation) {
    super(itemViewElevation);
  }

  protected float getAnimationTranslationX(View itemView) {
    return -dpToPx(32, itemView.getContext());
  }

  @Override
  protected float getAnimationTranslationY(View itemView) {
    return 0;
  }
}
