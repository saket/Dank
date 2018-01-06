package me.saket.dank.utils.itemanimators;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

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

  @Override
  public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, @NonNull List<Object> payloads) {
    boolean hasPayloads = !payloads.isEmpty();
    return hasPayloads || super.canReuseUpdatedViewHolder(viewHolder, payloads);
  }
}
