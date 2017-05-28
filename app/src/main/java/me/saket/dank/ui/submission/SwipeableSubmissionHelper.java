package me.saket.dank.ui.submission;

import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import net.dean.jraw.models.Submission;

import me.saket.dank.R;
import me.saket.dank.data.SubmissionManager;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.RecyclerViewArrayAdapter;
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
public class SwipeableSubmissionHelper implements SwipeableLayout.SwipeActionIconProvider {

  private static final String ACTION_NAME_SAVE = "Save";
  private static final String ACTION_NAME_UNSAVE = "UnSave";
  private static final String ACTION_NAME_OPTIONS = "Options";
  private static final String ACTION_NAME_UPVOTE = "Upvote";
  private static final String ACTION_NAME_DOWNVOTE = "Downvote";

  private final SwipeActions swipeActionsWithUnsave;
  private final SwipeActions swipeActionsWithSave;
  private final SubmissionManager submissionManager;

  public SwipeableSubmissionHelper(SubmissionManager submissionManager) {
    this.submissionManager = submissionManager;

    SwipeAction saveSwipeAction = SwipeAction.create(ACTION_NAME_SAVE, R.color.list_item_swipe_save, 1f);
    SwipeAction unSaveSwipeAction = SwipeAction.create(ACTION_NAME_UNSAVE, R.color.list_item_swipe_save, 1f);
    SwipeAction moreOptionsSwipeAction = SwipeAction.create(ACTION_NAME_OPTIONS, R.color.list_item_swipe_more_options, 1f);
    SwipeAction downvoteSwipeAction = SwipeAction.create(ACTION_NAME_DOWNVOTE, R.color.list_item_swipe_downvote, 1f);
    SwipeAction upvoteSwipeAction = SwipeAction.create(ACTION_NAME_UPVOTE, R.color.list_item_swipe_upvote, 1f);

    swipeActionsWithUnsave = SwipeActions.builder()
        .startActions(SwipeActionsHolder.builder()
            .add(unSaveSwipeAction)
            .add(moreOptionsSwipeAction)
            .build())
        .endActions(SwipeActionsHolder.builder()
            .add(downvoteSwipeAction)
            .add(upvoteSwipeAction)
            .build())
        .build();

    swipeActionsWithSave = SwipeActions.builder()
        .startActions(SwipeActionsHolder.builder()
            .add(saveSwipeAction)
            .add(moreOptionsSwipeAction)
            .build())
        .endActions(SwipeActionsHolder.builder()
            .add(downvoteSwipeAction)
            .add(upvoteSwipeAction)
            .build())
        .build();
  }

  public void attachToRecyclerView(RecyclerView recyclerView) {
    RecyclerSwipeListener swipeListener = new RecyclerSwipeListener(recyclerView);
    recyclerView.addOnItemTouchListener(swipeListener);
  }

  @CheckResult
  public <T extends Submission, VH extends RecyclerView.ViewHolder & ViewHolderWithSwipeActions> RecyclerView.Adapter wrapAdapter(
      RecyclerViewArrayAdapter<T, VH> adapterToWrap)
  {
    return new SimpleRecyclerAdapterWrapper<VH>(adapterToWrap) {
      @Override
      public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        VH holder = adapterToWrap.onCreateViewHolder(parent, viewType);
        holder.getSwipeableLayout().setSwipeActionIconProvider(SwipeableSubmissionHelper.this);
        return holder;
      }

      @Override
      public void onBindViewHolder(VH holder, int position) {
        adapterToWrap.onBindViewHolder(holder, position);
        Submission submission = adapterToWrap.getItem(position);
        SwipeableLayout swipeableLayout = holder.getSwipeableLayout();

        determineAndSetSwipeActions(submission, swipeableLayout);

        swipeableLayout.setOnPerformSwipeActionListener(action -> {
          swipeableLayout.playRippleAnimation(action);
          performSwipeAction(action, submission);

          // Update swipe actions in case submission was saved.
          determineAndSetSwipeActions(submission, swipeableLayout);
        });
      }

      private void determineAndSetSwipeActions(Submission submission, SwipeableLayout swipeableLayout) {
        boolean isSubmissionSaved = submissionManager.isSaved(submission);
        swipeableLayout.setSwipeActions(isSubmissionSaved ? swipeActionsWithUnsave : swipeActionsWithSave);
      }
    };
  }

  @Override
  public void showSwipeActionIcon(SwipeActionIconView imageView, @Nullable SwipeAction oldAction, SwipeAction newAction) {
    switch (newAction.name()) {
      case ACTION_NAME_OPTIONS:
        imageView.setRotation(0);
        imageView.setImageResource(R.drawable.ic_more_vert_24dp);
        break;

      case ACTION_NAME_SAVE:
        imageView.setRotation(0);
        imageView.setImageResource(R.drawable.ic_star_24dp);
        break;

      case ACTION_NAME_UNSAVE:
        imageView.setRotation(0);
        imageView.setImageResource(R.drawable.ic_star_border_24dp);
        break;

      case ACTION_NAME_UPVOTE:
        if (oldAction != null && ACTION_NAME_DOWNVOTE.equals(oldAction.name())) {
          animateFlipImageView(imageView);
        } else {
          imageView.setRotation(0);
          imageView.setImageResource(R.drawable.ic_arrow_upward_24dp);
        }
        break;

      case ACTION_NAME_DOWNVOTE:
        if (oldAction != null && ACTION_NAME_UPVOTE.equals(oldAction.name())) {
          animateFlipImageView(imageView);
        } else {
          imageView.setRotation(0);
          imageView.setImageResource(R.drawable.ic_arrow_downward_24dp);
        }
        break;

      default:
        throw new UnsupportedOperationException("Unknown swipe action: " + newAction);
    }
  }

  private void animateFlipImageView(SwipeActionIconView imageView) {
    imageView.animate().rotationBy(180).setInterpolator(Animations.INTERPOLATOR).setDuration(200).start();
  }

  private void performSwipeAction(SwipeAction action, Submission submission) {
    Timber.i("Action: %s", action.name());

    switch (action.name()) {
      case ACTION_NAME_OPTIONS:
        break;

      case ACTION_NAME_SAVE:
        submissionManager.save(submission);
        break;

      case ACTION_NAME_UNSAVE:
        submissionManager.unSave(submission);
        break;

      case ACTION_NAME_UPVOTE:
        // TODO: Remove upvote if submission was already upvoted.
        break;

      case ACTION_NAME_DOWNVOTE:
        // TODO: Remove downvote if submission was already downvoted.
        break;

      default:
        throw new UnsupportedOperationException("Unknown swipe action: " + action);
    }
  }
}
