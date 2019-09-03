package me.saket.dank.ui.subreddit;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.lifecycle.LifecycleOwnerActivity;
import me.saket.dank.utils.lifecycle.LifecycleOwnerViews;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

/**
 * Shows welcome text with typewriter animation.
 */
public class WelcomeToDankView extends FrameLayout {

  private final ColorDrawable background;
  private final LifecycleOwnerViews.Streams lifecycle;
  private final WelcomeTextView textView;

  public WelcomeToDankView(Context context) {
    super(context);
    setElevation(99);

    LifecycleOwnerActivity parentLifecycleOwner = (LifecycleOwnerActivity) getContext();
    lifecycle = LifecycleOwnerViews.create(this, parentLifecycleOwner.lifecycle());

    textView = new WelcomeTextView(context);
    addView(textView, textView.layoutParamsForFrameLayout());

    background = new ColorDrawable(ContextCompat.getColor(context, R.color.window_background));
    setBackground(background);
  }

  public Completable showAnimation() {
    Completable removeBackgroundCompletable = Completable.fromAction(() -> {
      ValueAnimator animator = ObjectAnimator.ofInt(255, 0);
      animator.addUpdateListener(animation -> {
        background.setAlpha(((int) animation.getAnimatedValue()));
        setBackground(background);
      });
      animator.setDuration(Animations.TRANSITION_ANIM_DURATION);
      animator.setInterpolator(Animations.INTERPOLATOR);
      animator.start();
    });

    Completable removeViewCompletable = Completable.create(emitter -> animate()
        .alpha(0f)
        .setDuration(100)
        .withEndAction(() -> ((ViewGroup) getParent()).removeView(this))
        .withEndAction(() -> emitter.onComplete()));

    return lifecycle.viewAttaches()
        .take(1)
        .ignoreElements()
        .andThen(textView.animateText2())
        .andThen(Completable.timer(1, TimeUnit.SECONDS, mainThread()))
        .andThen(removeBackgroundCompletable)
        .andThen(Completable.timer(1, TimeUnit.SECONDS, mainThread()))
        .andThen(removeViewCompletable);
  }

  public RelativeLayout.LayoutParams layoutParamsForRelativeLayout() {
    return new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
  }

  public static class WelcomeTextView extends AppCompatTextView {
    private final int ANIM_DURATION_PER_LETTER = 100;

    public WelcomeTextView(Context context) {
      super(context);

      setLineSpacing(0, 1.1f);
      setTextSize(getResources().getDimensionPixelSize(R.dimen.textsize24));
      setTextColor(ContextCompat.getColor(context, R.color.color_accent));
      setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
    }

    public FrameLayout.LayoutParams layoutParamsForFrameLayout() {
      FrameLayout.LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
      params.gravity = Gravity.CENTER;
      return params;
    }

    public Completable animateText2() {
      String text = getResources().getString(R.string.dank_welcome);

      return Observable.range(0, text.length())
          .zipWith(Observable.interval(ANIM_DURATION_PER_LETTER, TimeUnit.MILLISECONDS, mainThread()), (revealDistance, o) -> revealDistance)
          .flatMapCompletable(revealLength -> Completable.fromAction(() -> setText(text.substring(0, revealLength + 1))));
    }
  }
}
