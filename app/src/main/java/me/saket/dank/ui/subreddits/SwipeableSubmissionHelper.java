package me.saket.dank.ui.subreddits;

import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import io.reactivex.schedulers.Schedulers;
import me.saket.dank.R;
import me.saket.dank.data.SubmissionManager;
import me.saket.dank.data.VotingManager;
import me.saket.dank.ui.submission.SimpleRecyclerAdapterWrapper;
import me.saket.dank.ui.subreddits.SubmissionsAdapter.SubmissionViewHolder;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.RecyclerViewArrayAdapter;
import me.saket.dank.widgets.swipe.RecyclerSwipeListener;
import me.saket.dank.widgets.swipe.SwipeAction;
import me.saket.dank.widgets.swipe.SwipeActionIconView;
import me.saket.dank.widgets.swipe.SwipeActions;
import me.saket.dank.widgets.swipe.SwipeActionsHolder;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import timber.log.Timber;

/**
 * Controls swipe actions on submissions.
 */
public class SwipeableSubmissionHelper implements SwipeableLayout.SwipeActionIconProvider {

  private static final String ACTION_NAME_SAVE = "Save";
  private static final String ACTION_NAME_UNSAVE = "UnSave";
  private static final String ACTION_NAME_OPTIONS = "Options";
  static final String ACTION_NAME_UPVOTE = "Upvote";
  static final String ACTION_NAME_DOWNVOTE = "Downvote";

  private final SwipeActions swipeActionsWithUnsave;
  private final SwipeActions swipeActionsWithSave;
  private final SubmissionManager submissionManager;
  private VotingManager votingManager;

  public SwipeableSubmissionHelper(SubmissionManager submissionManager, VotingManager votingManager) {
    this.submissionManager = submissionManager;
    this.votingManager = votingManager;

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
  public RecyclerViewArrayAdapter<Submission, SubmissionViewHolder> wrapAdapter(SubmissionsAdapter adapterToWrap) {
    return new SimpleRecyclerAdapterWrapper<Submission, SubmissionViewHolder>(adapterToWrap) {
      @Override
      public SubmissionViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        SubmissionViewHolder holder = adapterToWrap.onCreateViewHolder(parent, viewType);
        holder.getSwipeableLayout().setSwipeActionIconProvider(SwipeableSubmissionHelper.this);
        return holder;
      }

      @Override
      public void onBindViewHolder(SubmissionViewHolder holder, int position) {
        adapterToWrap.onBindViewHolder(holder, position);
        Submission submission = adapterToWrap.getItem(position);
        SwipeableLayout swipeableLayout = holder.getSwipeableLayout();

        swipeableLayout.setSwipeActions(determineSwipeActions(submission));

        swipeableLayout.setOnPerformSwipeActionListener(action -> {
          performSwipeAction(action, submission);
          swipeableLayout.playRippleAnimation(action);  // TODO: Specify ripple direction.

          // We should ideally only be updating the backing data-set and let onBind() handle the
          // changes, but RecyclerView's item animator reset's the View's x-translation which we
          // don't want. So we manually update the Views here.
          adapterToWrap.onBindViewHolder(holder, position);
        });
      }

      private SwipeActions determineSwipeActions(Submission submission) {
        boolean isSubmissionSaved = submissionManager.isSaved(submission);
        return isSubmissionSaved ? swipeActionsWithUnsave : swipeActionsWithSave;
      }
    };
  }

  @Override
  public void showSwipeActionIcon(SwipeActionIconView imageView, @Nullable SwipeAction oldAction, SwipeAction newAction) {
    switch (newAction.name()) {
      case ACTION_NAME_OPTIONS:
        resetIconRotation(imageView);
        imageView.setImageResource(R.drawable.ic_more_vert_24dp);
        break;

      case ACTION_NAME_SAVE:
        resetIconRotation(imageView);
        imageView.setImageResource(R.drawable.ic_star_24dp);
        break;

      case ACTION_NAME_UNSAVE:
        resetIconRotation(imageView);
        imageView.setImageResource(R.drawable.ic_star_border_24dp);
        break;

      case ACTION_NAME_UPVOTE:
        if (oldAction != null && ACTION_NAME_DOWNVOTE.equals(oldAction.name())) {
          imageView.setRotation(180);   // We want to play a circular animation if the user keeps switching between upvote and downvote.
          imageView.animate().rotationBy(180).setInterpolator(Animations.INTERPOLATOR).setDuration(200).start();
        } else {
          resetIconRotation(imageView);
          imageView.setImageResource(R.drawable.ic_arrow_upward_24dp);
        }
        break;

      case ACTION_NAME_DOWNVOTE:
        if (oldAction != null && ACTION_NAME_UPVOTE.equals(oldAction.name())) {
          imageView.setRotation(0);
          imageView.animate().rotationBy(180).setInterpolator(Animations.INTERPOLATOR).setDuration(200).start();
        } else {
          resetIconRotation(imageView);
          imageView.setImageResource(R.drawable.ic_arrow_downward_24dp);
        }
        break;

      default:
        throw new UnsupportedOperationException("Unknown swipe action: " + newAction);
    }
  }

  private void resetIconRotation(SwipeActionIconView imageView) {
    imageView.animate().cancel();
    imageView.setRotation(0);
  }

  private void performSwipeAction(SwipeAction action, Submission submission) {
    switch (action.name()) {
      case ACTION_NAME_OPTIONS:
        Timber.i("Action: %s", action.name());
        break;

      case ACTION_NAME_SAVE:
        submissionManager.markAsSaved(submission);
        Timber.i("Action: %s", action.name());
        break;

      case ACTION_NAME_UNSAVE:
        submissionManager.markAsUnsaved(submission);
        Timber.i("Action: %s", action.name());
        break;

      case ACTION_NAME_UPVOTE: {
        VoteDirection currentVoteDirection = votingManager.getPendingVote(submission, submission.getVote());
        VoteDirection newVoteDirection = currentVoteDirection == VoteDirection.UPVOTE ? VoteDirection.NO_VOTE : VoteDirection.UPVOTE;
        votingManager.voteWithAutoRetry(submission, newVoteDirection)
            .subscribeOn(Schedulers.io())
            .subscribe();
        break;
      }

      case ACTION_NAME_DOWNVOTE: {
        VoteDirection currentVoteDirection = votingManager.getPendingVote(submission, submission.getVote());
        VoteDirection newVoteDirection = currentVoteDirection == VoteDirection.DOWNVOTE ? VoteDirection.NO_VOTE : VoteDirection.DOWNVOTE;
        votingManager.voteWithAutoRetry(submission, newVoteDirection)
            .subscribeOn(Schedulers.io())
            .subscribe();
        break;
      }

      default:
        throw new UnsupportedOperationException("Unknown swipe action: " + action);
    }
  }
}
