package me.saket.dank.utils.itemanimators;

import android.view.View;

public class SubmissionCommentsItemAnimator extends SlideAlphaAnimator<SubmissionCommentsItemAnimator> {

  public SubmissionCommentsItemAnimator(int itemViewElevation) {
    super(itemViewElevation);
  }

  @Override
  protected float getAnimationTranslationX(View itemView) {
    return 0;
  }

  protected float getAnimationTranslationY(View itemView) {
    return -dpToPx(32, itemView.getContext());
  }
}
