package me.saket.dank.ui.submission;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import me.saket.dank.R;
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

  private static final String ACTION_SAVE = "SAVE";
  private static final String ACTION_OPTIONS = "OPTIONS";
  private static final String ACTION_UPVOTE = "UPVOTE";
  private static final String ACTION_DOWNVOTE = "DOWNVOTE";

  private final SwipeActions swipeActions;

  public SwipeableSubmissionManager(Context context) {
    this.swipeActions = SwipeActions.builder()
        .startActions(SwipeActionsHolder.builder()
            .add(SwipeAction.create(ACTION_SAVE, Color.BLUE, 1f))
            .add(SwipeAction.create(ACTION_OPTIONS, Color.CYAN, 1f))
            .build())
        .endActions(SwipeActionsHolder.builder()
            .add(SwipeAction.create(ACTION_DOWNVOTE, ContextCompat.getColor(context, R.color.list_item_swipe_downvote), 1f))
            .add(SwipeAction.create(ACTION_UPVOTE, ContextCompat.getColor(context, R.color.list_item_swipe_upvote), 1f))
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
      case ACTION_SAVE:
        imageView.setImageResource(R.drawable.ic_adb_24dp);
        break;

      case ACTION_OPTIONS:
        imageView.setImageResource(R.drawable.ic_done_all_24dp);
        break;
    }
  }
}
