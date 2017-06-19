package me.saket.dank.utils.itemanimators;

import android.content.Context;
import android.support.annotation.Px;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;

import timber.log.Timber;

/**
 * Created by mikepenz on 08.01.16.
 */
public class SlideDownAlphaAnimator extends me.saket.dank.utils.itemanimators.DefaultAnimator<SlideDownAlphaAnimator> {

    @Override
    public void addAnimationPrepare(RecyclerView.ViewHolder holder) {
        ViewCompat.setTranslationY(holder.itemView, getAnimationTranslationY(holder.itemView));
        ViewCompat.setAlpha(holder.itemView, getMinAlpha());
        holder.itemView.setElevation(0);
    }

    private float getMinAlpha() {
        return 0f;
    }

    private float getAnimationTranslationY(View itemView) {
        return -dpToPx(4*8, itemView.getContext());
    }

    @Px
    public static int dpToPx(float dpValue, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.getResources().getDisplayMetrics());
    }

    @Override
    public ViewPropertyAnimatorCompat addAnimation(RecyclerView.ViewHolder holder) {
        return ViewCompat.animate(holder.itemView)
            .translationY(0)
            .alpha(1)
            .setDuration(getMoveDuration())
            .setInterpolator(getInterpolator());
    }

    @Override
    public void addAnimationCleanup(RecyclerView.ViewHolder holder) {
        ViewCompat.setTranslationY(holder.itemView, 0);
        ViewCompat.setAlpha(holder.itemView, 1);
        holder.itemView.setElevation(dpToPx(1, holder.itemView.getContext()));
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
    }

    @Override
    public ViewPropertyAnimatorCompat removeAnimation(RecyclerView.ViewHolder holder) {
        final ViewPropertyAnimatorCompat animation = ViewCompat.animate(holder.itemView);
        return animation
            .setDuration(getRemoveDuration())
            .alpha(getMinAlpha())
            .translationY(getAnimationTranslationY(holder.itemView))
            .setInterpolator(getInterpolator());
    }

    @Override
    public void removeAnimationCleanup(RecyclerView.ViewHolder holder) {
        ViewCompat.setTranslationY(holder.itemView, 0);
        ViewCompat.setAlpha(holder.itemView, 1);
        holder.itemView.setElevation(dpToPx(1, holder.itemView.getContext()));
    }
}
