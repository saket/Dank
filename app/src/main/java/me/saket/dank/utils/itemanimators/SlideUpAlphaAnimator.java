package me.saket.dank.utils.itemanimators;

import android.graphics.drawable.Drawable;
import android.view.View;

public class SlideUpAlphaAnimator extends SlideAlphaAnimator<SlideUpAlphaAnimator> {

  public SlideUpAlphaAnimator(Drawable itemBackgroundDuringAnimation) {
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
