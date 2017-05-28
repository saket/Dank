package me.saket.dank.ui.submission;

import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import me.saket.dank.R;
import me.saket.dank.utils.Animations;
import me.saket.dank.widgets.swipe.RecyclerSwipeListener;
import me.saket.dank.widgets.swipe.SwipeAction;
import me.saket.dank.widgets.swipe.SwipeActionIconView;
import me.saket.dank.widgets.swipe.SwipeActions;
import me.saket.dank.widgets.swipe.SwipeActionsHolder;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions;
import timber.log.Timber;

/**
 * Controls swipe actions on submissions.
 */
public class SwipeableSubmissionManager implements SwipeableLayout.SwipeActionIconProvider {

  private static final String ACTION_NAME_SAVE = "Save";
  private static final String ACTION_NAME_OPTIONS = "Options";
  private static final String ACTION_NAME_UPVOTE = "Upvote";
  private static final String ACTION_NAME_DOWNVOTE = "Downvote";

  private final SwipeActions swipeActions;

  public SwipeableSubmissionManager(Context context) {
    this.swipeActions = SwipeActions.builder()
        .startActions(SwipeActionsHolder.builder()
            .add(SwipeAction.create(ACTION_NAME_SAVE, ContextCompat.getColor(context, R.color.list_item_swipe_save), 1f))
            .add(SwipeAction.create(ACTION_NAME_OPTIONS, ContextCompat.getColor(context, R.color.list_item_swipe_more_options), 1f))
            .build())
        .endActions(SwipeActionsHolder.builder()
            .add(SwipeAction.create(ACTION_NAME_DOWNVOTE, ContextCompat.getColor(context, R.color.list_item_swipe_downvote), 1f))
            .add(SwipeAction.create(ACTION_NAME_UPVOTE, ContextCompat.getColor(context, R.color.list_item_swipe_upvote), 1f))
            .build())
        .build();
  }

  public void attachToRecyclerView(RecyclerView recyclerView) {
    RecyclerSwipeListener swipeListener = new RecyclerSwipeListener(recyclerView);
    recyclerView.addOnItemTouchListener(swipeListener);
  }

  @CheckResult
  public <VH extends RecyclerView.ViewHolder & ViewHolderWithSwipeActions> RecyclerView.Adapter<VH> wrapAdapter(RecyclerView.Adapter<VH> adapterToWrap) {
    return new SimpleRecyclerAdapterWrapper<VH>(adapterToWrap) {
      @Override
      public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        VH holder = adapterToWrap.onCreateViewHolder(parent, viewType);
        holder.getSwipeableLayout().setSwipeActions(swipeActions);
        holder.getSwipeableLayout().setSwipeActionIconProvider(SwipeableSubmissionManager.this);
        return holder;
      }

      @Override
      public void onBindViewHolder(VH holder, int position) {
        adapterToWrap.onBindViewHolder(holder, position);
        holder.getSwipeableLayout().setOnPerformSwipeActionListener(action -> {
          Timber.i("Action: %s", action.name());
        });
      }
    };
  }

  @Override
  public void showSwipeActionIcon(SwipeActionIconView imageView, @Nullable SwipeAction oldAction, SwipeAction newAction) {
    switch (newAction.name()) {
      case ACTION_NAME_OPTIONS:
        imageView.setImageResource(R.drawable.ic_more_vert_24dp);
        break;

      case ACTION_NAME_SAVE:
        imageView.setImageResource(R.drawable.ic_star_24dp);
        break;

      case ACTION_NAME_UPVOTE:
        if (oldAction != null && ACTION_NAME_DOWNVOTE.equals(oldAction.name())) {
          animateFlipImageView(imageView);
        } else {
          imageView.setImageResource(R.drawable.ic_arrow_upward_24dp);
        }
        break;

      case ACTION_NAME_DOWNVOTE:
        if (oldAction != null && ACTION_NAME_UPVOTE.equals(oldAction.name())) {
          animateFlipImageView(imageView);
        } else {
          imageView.setImageResource(R.drawable.ic_arrow_downward_24dp);
        }
        break;
    }
  }

  private static void animateFlipImageView(SwipeActionIconView imageView) {
    imageView.animate().rotationBy(180).setInterpolator(Animations.INTERPOLATOR).setDuration(200).start();
  }
}
