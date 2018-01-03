package me.saket.dank.utils.itemanimators;

import android.graphics.drawable.Drawable;
import android.view.View;

import me.saket.dank.utils.Optional;

public class SlideUpAlphaAnimator extends SlideAlphaAnimator<SlideUpAlphaAnimator> {

  public SlideUpAlphaAnimator(Drawable itemBackgroundDuringAnimation) {
    super(0, Optional.of(itemBackgroundDuringAnimation));
  }

  @Override
  protected float getAnimationTranslationX(View itemView) {
    return 0;
  }

  protected float getAnimationTranslationY(View itemView) {
    return dpToPx(32, itemView.getContext());
  }
}
