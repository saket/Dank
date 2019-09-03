package me.saket.dank.ui.subscriptions;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.utils.RecyclerViewArrayAdapter;

/**
 * Adapter for displaying a list of subreddits.
 */
public class SubredditAdapter extends RecyclerViewArrayAdapter<SubredditSubscription, SubredditAdapter.SubredditViewHolder> {

  private OnSubredditClickListener clickListener;
  private @Nullable SubredditSubscription highlightedSubscription;
  private Runnable onHighlightAnimEndRunnable = () -> highlightedSubscription = null;

  public interface OnSubredditClickListener {
    void onClickSubreddit(SubredditSubscription subscription, View subredditItemView);
  }

  @Inject
  public SubredditAdapter() {
    setHasStableIds(true);
    temporarilyHighlight(null);
  }

  public void setOnSubredditClickListener(OnSubredditClickListener listener) {
    clickListener = listener;
  }

  public void temporarilyHighlight(@Nullable SubredditSubscription subscription) {
    highlightedSubscription = subscription;
  }

  @Override
  protected SubredditViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    return SubredditViewHolder.create(inflater, parent);
  }

  @Override
  public void onBindViewHolder(SubredditViewHolder holder, int position) {
    SubredditSubscription subSubscription = getItem(position);

    boolean isHighlighted = subSubscription.equals(highlightedSubscription);
    holder.bind(subSubscription, isHighlighted, onHighlightAnimEndRunnable);

    //noinspection CodeBlock2Expr
    holder.itemView.setOnClickListener(itemView -> {
      clickListener.onClickSubreddit(subSubscription, itemView);
    });
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).hashCode();
  }

  static class SubredditViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.item_subreddit_name) Button subredditNameButton;
    @BindColor(R.color.subredditpicker_subreddit_button_text_normal) int normalTextColor;
    @BindColor(R.color.subredditpicker_subreddit_button_text_hidden) int hiddenTextColor;
    @BindColor(R.color.color_accent) int highlightedTextColor;

    public static SubredditViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new SubredditViewHolder(inflater.inflate(R.layout.list_item_subreddit, parent, false));
    }

    public SubredditViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    public void bind(SubredditSubscription subreddit, boolean isHighlighted, Runnable onHighlightEndRunnable) {
      subredditNameButton.setText(subreddit.name());
      subredditNameButton.getBackground().setAlpha(subreddit.isHidden() ? 100 : 255);

      int buttonTextColor = subreddit.isHidden() ? hiddenTextColor : normalTextColor;

      if (isHighlighted) {
        playHighlightAnimation(onHighlightEndRunnable, buttonTextColor);
      } else {
        subredditNameButton.setTextColor(buttonTextColor);
      }
    }

    /**
     * Note: ObjectAnimator keeps a weak reference to the target View, so there's no need to worry of a leak here.
     */
    private void playHighlightAnimation(Runnable onEndRunnable, int textColorToRestore) {
      WeakReference<Runnable> onEndRunnableRef = new WeakReference<>(onEndRunnable);

      ObjectAnimator highlightAnimator = ObjectAnimator.ofArgb(subredditNameButton, "textColor", textColorToRestore, highlightedTextColor);
      highlightAnimator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          if (onEndRunnableRef.get() != null) {
            onEndRunnableRef.get().run();

            ObjectAnimator fadeOutAnimator = ObjectAnimator.ofArgb(subredditNameButton, "textColor", highlightedTextColor,
                textColorToRestore);
            fadeOutAnimator.setStartDelay(2000);
            fadeOutAnimator.start();
          }
        }
      });
      highlightAnimator.start();
    }
  }
}
