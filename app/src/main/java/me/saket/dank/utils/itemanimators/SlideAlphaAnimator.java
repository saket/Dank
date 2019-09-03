package me.saket.dank.utils.itemanimators;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Px;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorCompat;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Copied from https://github.com/mikepenz/ItemAnimators. Modified for our use.
 */
public abstract class SlideAlphaAnimator<T> extends DefaultAnimator<T> {

  private final int itemViewElevation;

  protected SlideAlphaAnimator(int itemViewElevation) {
    this.itemViewElevation = itemViewElevation;
  }

  private float getMinAlpha() {
    return 0f;
  }

  public T withRemoveDuration(long duration) {
    setRemoveDuration(duration);
    //noinspection unchecked
    return (T) this;
  }

  public T withAddDuration(long duration) {
    setAddDuration(duration);
    //noinspection unchecked
    return (T) this;
  }

  protected abstract float getAnimationTranslationY(View itemView);

  protected abstract float getAnimationTranslationX(View itemView);

  @Px
  public static int dpToPx(float dpValue, Context context) {
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.getResources().getDisplayMetrics());
  }

  @Override
  public void addAnimationPrepare(RecyclerView.ViewHolder holder) {
    ViewCompat.setTranslationX(holder.itemView, getAnimationTranslationX(holder.itemView));
    ViewCompat.setTranslationY(holder.itemView, getAnimationTranslationY(holder.itemView));
    ViewCompat.setAlpha(holder.itemView, getMinAlpha());
    holder.itemView.setElevation(0);
//    if (itemViewBackgroundDuringAnimation.isPresent()) {
//      holder.itemView.setBackground(itemViewBackgroundDuringAnimation.get());
//    }
  }

  @Override
  public ViewPropertyAnimatorCompat addAnimation(RecyclerView.ViewHolder holder) {
    return ViewCompat.animate(holder.itemView)
        .translationX(0)
        .translationY(0)
        .alpha(1)
        .setDuration(getMoveDuration())
        .setInterpolator(getInterpolator());
  }

  @Override
  public void addAnimationCleanup(RecyclerView.ViewHolder holder) {
    ViewCompat.setTranslationX(holder.itemView, 0);
    ViewCompat.setTranslationY(holder.itemView, 0);
    ViewCompat.setAlpha(holder.itemView, 1);
    holder.itemView.setElevation(itemViewElevation);
//    if (itemViewBackgroundDuringAnimation.isPresent()) {
//      holder.itemView.setBackground(null);
//    }
  }

  @Override
  public long getAddDelay(long remove, long move, long change) {
    return 0;
  }

  @Override
  public long getRemoveDelay(long remove, long move, long change) {
    return remove / 2;
  }

  @Override
  public void removeAnimationPrepare(RecyclerView.ViewHolder holder) {
    holder.itemView.setElevation(0);
//    if (itemViewBackgroundDuringAnimation.isPresent()) {
//      holder.itemView.setBackground(itemViewBackgroundDuringAnimation.get());
//    }
  }

  @Override
  public ViewPropertyAnimatorCompat removeAnimation(RecyclerView.ViewHolder holder) {
    final ViewPropertyAnimatorCompat animation = ViewCompat.animate(holder.itemView);
    return animation
        .setDuration(getRemoveDuration())
        .alpha(getMinAlpha())
        .translationX(getAnimationTranslationX(holder.itemView))
        .translationY(getAnimationTranslationY(holder.itemView))
        .setInterpolator(getInterpolator());
  }

  @Override
  public void removeAnimationCleanup(RecyclerView.ViewHolder holder) {
    ViewCompat.setTranslationX(holder.itemView, 0);
    ViewCompat.setTranslationY(holder.itemView, 0);
    ViewCompat.setAlpha(holder.itemView, 1);
    holder.itemView.setElevation(itemViewElevation);
//    if (itemViewBackgroundDuringAnimation.isPresent()) {
//      holder.itemView.setBackground(null);
//    }
  }

  @Override
  public void onMoveStarting(RecyclerView.ViewHolder holder) {
//    if (itemViewBackgroundDuringAnimation.isPresent()) {
//      holder.itemView.setBackground(itemViewBackgroundDuringAnimation.get());
//    }
  }

  @Override
  public void onMoveFinished(RecyclerView.ViewHolder holder) {
//    if (itemViewBackgroundDuringAnimation.isPresent()) {
//      holder.itemView.setBackground(null);
//    }
  }
}
