package me.saket.dank.utils.itemanimators;

import android.graphics.drawable.Drawable;
import android.view.View;

import me.saket.dank.utils.Optional;

public class SubmissionCommentsItemAnimator extends SlideAlphaAnimator<SubmissionCommentsItemAnimator> {

  /**
   * @param itemViewBackgroundDuringAnimation See {@link SlideAlphaAnimator}.
   */
  public SubmissionCommentsItemAnimator(int itemViewElevation, Drawable itemViewBackgroundDuringAnimation) {
    super(itemViewElevation, Optional.of(itemViewBackgroundDuringAnimation));
  }

  @Override
  protected float getAnimationTranslationX(View itemView) {
    return 0;
  }

  protected float getAnimationTranslationY(View itemView) {
    return -dpToPx(32, itemView.getContext());
  }
}
