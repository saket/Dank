package me.saket.dank.utils;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import androidx.annotation.Nullable;
import androidx.transition.ChangeBounds;
import androidx.transition.Fade;
import androidx.transition.TransitionSet;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import android.util.ArrayMap;
import android.view.animation.Interpolator;

import java.util.ArrayList;

/**
 * Animation utilities.
 */
public class Animations {

  public static final int TRANSITION_ANIM_DURATION = 200;
  public static final Interpolator INTERPOLATOR = new FastOutSlowInInterpolator();

  public static final Interpolator FAST_OUT_SLOW_IN_INTERPOLATOR = INTERPOLATOR;
  public static final Interpolator FAST_OUT_LINEAR_IN_INTERPOLATOR = new FastOutLinearInInterpolator();

  public static TransitionSet transitions() {
    return new TransitionSet()
        .addTransition(new ChangeBounds().setInterpolator(Animations.INTERPOLATOR))
        .addTransition(new Fade(Fade.IN).setInterpolator(Animations.INTERPOLATOR))
        .addTransition(new Fade(Fade.OUT).setInterpolator(Animations.INTERPOLATOR))
        .setOrdering(TransitionSet.ORDERING_TOGETHER)
        .setDuration(TRANSITION_ANIM_DURATION);
  }

  /**
   * https://halfthought.wordpress.com/2014/11/07/reveal-transition/
   * <p/>
   * Interrupting Activity transitions can yield an OperationNotSupportedException when the
   * transition tries to pause the animator. Yikes! We can fix this by wrapping the Animator:
   */
  public static class NoPauseAnimator extends Animator {
    private final Animator mAnimator;
    private final ArrayMap<AnimatorListener, AnimatorListener> mListeners = new ArrayMap<>();

    public NoPauseAnimator(Animator animator) {
      mAnimator = animator;
    }

    @Override
    public void addListener(AnimatorListener listener) {
      AnimatorListener wrapper = new AnimatorListenerWrapper(this, listener);
      if (!mListeners.containsKey(listener)) {
        mListeners.put(listener, wrapper);
        mAnimator.addListener(wrapper);
      }
    }

    @Override
    public void cancel() {
      mAnimator.cancel();
    }

    @Override
    public void end() {
      mAnimator.end();
    }

    @Override
    public long getDuration() {
      return mAnimator.getDuration();
    }

    @Override
    public TimeInterpolator getInterpolator() {
      return mAnimator.getInterpolator();
    }

    @Override
    public void setInterpolator(TimeInterpolator timeInterpolator) {
      mAnimator.setInterpolator(timeInterpolator);
    }

    @Override
    public ArrayList<AnimatorListener> getListeners() {
      return new ArrayList<>(mListeners.keySet());
    }

    @Override
    public long getStartDelay() {
      return mAnimator.getStartDelay();
    }

    @Override
    public void setStartDelay(long delayMS) {
      mAnimator.setStartDelay(delayMS);
    }

    @Override
    public boolean isPaused() {
      return mAnimator.isPaused();
    }

    @Override
    public boolean isRunning() {
      return mAnimator.isRunning();
    }

    @Override
    public boolean isStarted() {
      return mAnimator.isStarted();
    }

        /* We don't want to override pause or resume methods because we don't want them
         * to affect mAnimator.
        public void pause();

        public void resume();

        public void addPauseListener(AnimatorPauseListener listener);

        public void removePauseListener(AnimatorPauseListener listener);
        */

    @Override
    public void removeAllListeners() {
      mListeners.clear();
      mAnimator.removeAllListeners();
    }

    @Override
    public void removeListener(AnimatorListener listener) {
      AnimatorListener wrapper = mListeners.get(listener);
      if (wrapper != null) {
        mListeners.remove(listener);
        mAnimator.removeListener(wrapper);
      }
    }

    @Override
    public Animator setDuration(long durationMS) {
      mAnimator.setDuration(durationMS);
      return this;
    }

    @Override
    public void setTarget(@Nullable Object target) {
      mAnimator.setTarget(target);
    }

    @Override
    public void setupEndValues() {
      mAnimator.setupEndValues();
    }

    @Override
    public void setupStartValues() {
      mAnimator.setupStartValues();
    }

    @Override
    public void start() {
      mAnimator.start();
    }
  }

  private static class AnimatorListenerWrapper implements Animator.AnimatorListener {
    private final Animator mAnimator;
    private final Animator.AnimatorListener mListener;

    AnimatorListenerWrapper(Animator animator, Animator.AnimatorListener listener) {
      mAnimator = animator;
      mListener = listener;
    }

    @Override
    public void onAnimationStart(Animator animator) {
      mListener.onAnimationStart(mAnimator);
    }

    @Override
    public void onAnimationEnd(Animator animator) {
      mListener.onAnimationEnd(mAnimator);
    }

    @Override
    public void onAnimationCancel(Animator animator) {
      mListener.onAnimationCancel(mAnimator);
    }

    @Override
    public void onAnimationRepeat(Animator animator) {
      mListener.onAnimationRepeat(mAnimator);
    }
  }
}
