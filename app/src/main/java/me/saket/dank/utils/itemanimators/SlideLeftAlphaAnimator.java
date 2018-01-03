package me.saket.dank.utils.itemanimators;

import android.view.View;

import me.saket.dank.utils.Optional;

public class SlideLeftAlphaAnimator extends SlideAlphaAnimator<SlideLeftAlphaAnimator> {

  public SlideLeftAlphaAnimator(int itemViewElevation) {
    super(itemViewElevation, Optional.empty());
  }

  protected float getAnimationTranslationX(View itemView) {
    return -dpToPx(32, itemView.getContext());
  }

  @Override
  protected float getAnimationTranslationY(View itemView) {
    return 0;
  }
}
