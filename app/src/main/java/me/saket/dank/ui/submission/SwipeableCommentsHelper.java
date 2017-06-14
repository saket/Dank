package me.saket.dank.ui.submission;

import android.support.annotation.CheckResult;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import io.reactivex.schedulers.Schedulers;
import me.saket.dank.R;
import me.saket.dank.data.SubmissionManager;
import me.saket.dank.data.VotingManager;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.RecyclerViewArrayAdapter;
import me.saket.dank.utils.SubmissionAdapterWithHeader;
import me.saket.dank.widgets.swipe.RecyclerSwipeListener;
import me.saket.dank.widgets.swipe.SwipeAction;
import me.saket.dank.widgets.swipe.SwipeActionIconView;
import me.saket.dank.widgets.swipe.SwipeActions;
import me.saket.dank.widgets.swipe.SwipeActionsHolder;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions;
import timber.log.Timber;

/**
 * Controls gesture actions on comments and submission details.
 */
public class SwipeableCommentsHelper {

  private static final String ACTION_NAME_SUBMISSION_SAVE = "Save";
  private static final String ACTION_NAME_SUBMISSION_UNSAVE = "UnSave";
  private static final String ACTION_NAME_SUBMISSION_OPTIONS = "Options";
  private static final String ACTION_NAME_SUBMISSION_UPVOTE = "Upvote";
  private static final String ACTION_NAME_SUBMISSION_DOWNVOTE = "Downvote";

  private static final String ACTION_NAME_COMMENT_REPLY = "CommentReply";
  private static final String ACTION_NAME_COMMENT_OPTIONS = "CommentOptions";
  private static final String ACTION_NAME_COMMENT_UPVOTE = "CommentUpvote";
  private static final String ACTION_NAME_COMMENT_DOWNVOTE = "CommentDownvote";

  private final SubmissionManager submissionManager;
  private final VotingManager votingManager;
  private final SwipeActions submissionSwipeActionsWithUnsave;
  private final SwipeActions submissionSwipeActionsWithSave;
  private final SwipeActions commentSwipeActions;

  public SwipeableCommentsHelper(SubmissionManager submissionManager, VotingManager votingManager) {
    this.submissionManager = submissionManager;
    this.votingManager = votingManager;

    SwipeAction saveSwipeAction = SwipeAction.create(ACTION_NAME_SUBMISSION_SAVE, R.color.list_item_swipe_save, 1f);
    SwipeAction unSaveSwipeAction = SwipeAction.create(ACTION_NAME_SUBMISSION_UNSAVE, R.color.list_item_swipe_save, 1f);
    SwipeAction moreOptionsSwipeAction = SwipeAction.create(ACTION_NAME_SUBMISSION_OPTIONS, R.color.list_item_swipe_more_options, 1f);
    SwipeAction downvoteSwipeAction = SwipeAction.create(ACTION_NAME_SUBMISSION_DOWNVOTE, R.color.list_item_swipe_downvote, 1f);
    SwipeAction upvoteSwipeAction = SwipeAction.create(ACTION_NAME_SUBMISSION_UPVOTE, R.color.list_item_swipe_upvote, 1f);

    submissionSwipeActionsWithUnsave = SwipeActions.builder()
        .startActions(SwipeActionsHolder.builder()
            .add(unSaveSwipeAction)
            .add(moreOptionsSwipeAction)
            .build())
        .endActions(SwipeActionsHolder.builder()
            .add(downvoteSwipeAction)
            .add(upvoteSwipeAction)
            .build())
        .build();

    submissionSwipeActionsWithSave = SwipeActions.builder()
        .startActions(SwipeActionsHolder.builder()
            .add(saveSwipeAction)
            .add(moreOptionsSwipeAction)
            .build())
        .endActions(SwipeActionsHolder.builder()
            .add(downvoteSwipeAction)
            .add(upvoteSwipeAction)
            .build())
        .build();

    commentSwipeActions = SwipeActions.builder()
        .startActions(SwipeActionsHolder.builder()
            .add(SwipeAction.create(ACTION_NAME_COMMENT_REPLY, R.color.list_item_swipe_reply, 1f))
            .add(SwipeAction.create(ACTION_NAME_COMMENT_OPTIONS, R.color.list_item_swipe_more_options, 1f))
            .build())
        .endActions(SwipeActionsHolder.builder()
            .add(SwipeAction.create(ACTION_NAME_COMMENT_DOWNVOTE, R.color.list_item_swipe_downvote, 1f))
            .add(SwipeAction.create(ACTION_NAME_COMMENT_UPVOTE, R.color.list_item_swipe_upvote, 1f))
            .build())
        .build();
  }

  public void attachToRecyclerView(RecyclerView recyclerView) {
    RecyclerSwipeListener swipeListener = new RecyclerSwipeListener(recyclerView);
    recyclerView.addOnItemTouchListener(swipeListener);
  }

  @CheckResult
  public RecyclerViewArrayAdapter<Object, RecyclerView.ViewHolder> wrapAdapter(SubmissionAdapterWithHeader adapterToWrap) {
    return new SimpleRecyclerAdapterWrapper<Object, RecyclerView.ViewHolder>(adapterToWrap) {
      @Override
      public RecyclerView.ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder = adapterToWrap.onCreateViewHolder(parent, viewType);
        if (holder instanceof ViewHolderWithSwipeActions) {
          SwipeableLayout swipeableLayout = ((ViewHolderWithSwipeActions) holder).getSwipeableLayout();

          if (holder instanceof SubmissionAdapterWithHeader.SubmissionHeaderViewHolder) {
            swipeableLayout.setSwipeActionIconProvider(submissionSwipeActionIconProvider);

          } else if (holder instanceof CommentsAdapter.UserCommentViewHolder) {
            swipeableLayout.setSwipeActionIconProvider(commentSwipeActionIconProvider);
          }
        }
        return holder;
      }

      @Override
      public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        adapterToWrap.onBindViewHolder(holder, position);

        if (holder instanceof SubmissionAdapterWithHeader.SubmissionHeaderViewHolder) {
          bindSwipeActionsForSubmission((SubmissionAdapterWithHeader.SubmissionHeaderViewHolder) holder, position);

        } else if (holder instanceof CommentsAdapter.UserCommentViewHolder) {
          bindSwipeActionsForComment(((CommentsAdapter.UserCommentViewHolder) holder), position);
        }
      }

      private void bindSwipeActionsForSubmission(SubmissionAdapterWithHeader.SubmissionHeaderViewHolder holder, int position) {
        Submission submission = (Submission) adapterToWrap.getItem(position);  // TODO.
        if (submission == null) {
          return;
        }

        SwipeableLayout swipeableLayout = holder.getSwipeableLayout();
        swipeableLayout.setSwipeActions(determineSwipeActions(submission));

        swipeableLayout.setOnPerformSwipeActionListener(action -> {
          performSubmissionSwipeAction(action, submission);
          swipeableLayout.playRippleAnimation(action);  // TODO: Specify ripple direction.

          // We should ideally only be updating the backing data-set and let onBind() handle the
          // changes, but RecyclerView's item animator reset's the View's x-translation which we
          // don't want. So we manually update the Views here.
          adapterToWrap.onBindViewHolder(holder, position);
        });
      }

      private SwipeActions determineSwipeActions(Submission submission) {
        boolean isSubmissionSaved = submissionManager.isSaved(submission);
        return isSubmissionSaved ? submissionSwipeActionsWithUnsave : submissionSwipeActionsWithSave;
      }

      private void bindSwipeActionsForComment(CommentsAdapter.UserCommentViewHolder holder, int position) {
        SwipeableLayout swipeableLayout = holder.getSwipeableLayout();
        swipeableLayout.setSwipeActions(commentSwipeActions);

        CommentNode commentNode = ((DankCommentNode) adapterToWrap.getItem(position)).commentNode();
        swipeableLayout.setOnPerformSwipeActionListener(action -> {
          performCommentSwipeAction(action, commentNode);
          swipeableLayout.playRippleAnimation(action);  // TODO: Specify ripple direction.
          adapterToWrap.onBindViewHolder(holder, position);
        });
      }
    };
  }

  private void performCommentSwipeAction(SwipeAction swipeAction, CommentNode commentNode) {
    // TODO.
    Timber.i("Action: %s", swipeAction.name());
  }

  private void performSubmissionSwipeAction(SwipeAction swipeAction, Submission submission) {
    switch (swipeAction.name()) {
      case ACTION_NAME_SUBMISSION_OPTIONS:
        Timber.i("Action: %s", swipeAction.name());
        break;

      case ACTION_NAME_SUBMISSION_SAVE:
        submissionManager.markAsSaved(submission);
        Timber.i("Action: %s", swipeAction.name());
        break;

      case ACTION_NAME_SUBMISSION_UNSAVE:
        submissionManager.markAsUnsaved(submission);
        Timber.i("Action: %s", swipeAction.name());
        break;

      case ACTION_NAME_SUBMISSION_UPVOTE: {
        VoteDirection currentVoteDirection = votingManager.getPendingOrDefaultVote(submission, submission.getVote());
        VoteDirection newVoteDirection = currentVoteDirection == VoteDirection.UPVOTE ? VoteDirection.NO_VOTE : VoteDirection.UPVOTE;
        votingManager.voteWithAutoRetry(submission, newVoteDirection)
            .subscribeOn(Schedulers.io())
            .subscribe();
        break;
      }

      case ACTION_NAME_SUBMISSION_DOWNVOTE: {
        VoteDirection currentVoteDirection = votingManager.getPendingOrDefaultVote(submission, submission.getVote());
        VoteDirection newVoteDirection = currentVoteDirection == VoteDirection.DOWNVOTE ? VoteDirection.NO_VOTE : VoteDirection.DOWNVOTE;
        votingManager.voteWithAutoRetry(submission, newVoteDirection)
            .subscribeOn(Schedulers.io())
            .subscribe();
        break;
      }

      default:
        throw new UnsupportedOperationException("Unknown swipe action: " + swipeAction);
    }
  }

  private SwipeableLayout.SwipeActionIconProvider submissionSwipeActionIconProvider = (imageView, oldAction, newAction) -> {
    switch (newAction.name()) {
      case ACTION_NAME_SUBMISSION_OPTIONS:
        resetIconRotation(imageView);
        imageView.setImageResource(R.drawable.ic_more_horiz_24dp);
        break;

      case ACTION_NAME_SUBMISSION_SAVE:
        resetIconRotation(imageView);
        imageView.setImageResource(R.drawable.ic_star_24dp);
        break;

      case ACTION_NAME_SUBMISSION_UNSAVE:
        resetIconRotation(imageView);
        imageView.setImageResource(R.drawable.ic_star_border_24dp);
        break;

      case ACTION_NAME_SUBMISSION_UPVOTE:
        if (oldAction != null && ACTION_NAME_SUBMISSION_DOWNVOTE.equals(oldAction.name())) {
          imageView.setRotation(180);   // We want to play a circular animation if the user keeps switching between upvote and downvote.
          imageView.animate().rotationBy(180).setInterpolator(Animations.INTERPOLATOR).setDuration(200).start();
        } else {
          resetIconRotation(imageView);
          imageView.setImageResource(R.drawable.ic_arrow_upward_24dp);
        }
        break;

      case ACTION_NAME_SUBMISSION_DOWNVOTE:
        if (oldAction != null && ACTION_NAME_SUBMISSION_UPVOTE.equals(oldAction.name())) {
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
  };

  private void resetIconRotation(SwipeActionIconView imageView) {
    imageView.animate().cancel();
    imageView.setRotation(0);
  }

  private SwipeableLayout.SwipeActionIconProvider commentSwipeActionIconProvider = (imageView, oldAction, newAction) -> {
    switch (newAction.name()) {
      case ACTION_NAME_COMMENT_OPTIONS:
        resetIconRotation(imageView);
        imageView.setImageResource(R.drawable.ic_more_horiz_24dp);
        break;

      case ACTION_NAME_COMMENT_REPLY:
        resetIconRotation(imageView);
        imageView.setImageResource(R.drawable.ic_reply_24dp);
        break;

      case ACTION_NAME_COMMENT_UPVOTE:
        if (oldAction != null && ACTION_NAME_COMMENT_DOWNVOTE.equals(oldAction.name())) {
          imageView.setRotation(180);
          imageView.animate().rotationBy(180).setInterpolator(Animations.INTERPOLATOR).setDuration(200).start();
        } else {
          resetIconRotation(imageView);
          imageView.setImageResource(R.drawable.ic_arrow_upward_24dp);
        }
        break;

      case ACTION_NAME_COMMENT_DOWNVOTE:
        if (oldAction != null && ACTION_NAME_COMMENT_UPVOTE.equals(oldAction.name())) {
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
  };
}
