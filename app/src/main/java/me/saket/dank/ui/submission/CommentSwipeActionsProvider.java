package me.saket.dank.ui.submission;

import net.dean.jraw.models.VoteDirection;

import javax.inject.Inject;

import io.reactivex.schedulers.Schedulers;
import me.saket.dank.R;
import me.saket.dank.data.OnLoginRequireListener;
import me.saket.dank.data.PostedOrInFlightContribution;
import me.saket.dank.data.VotingManager;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.utils.Animations;
import me.saket.dank.widgets.swipe.SwipeAction;
import me.saket.dank.widgets.swipe.SwipeActionIconView;
import me.saket.dank.widgets.swipe.SwipeActions;
import me.saket.dank.widgets.swipe.SwipeActionsHolder;
import me.saket.dank.widgets.swipe.SwipeTriggerRippleDrawable.RippleType;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.SwipeableLayout.SwipeActionIconProvider;
import timber.log.Timber;

/**
 * Controls gesture actions on comments and submission details.
 */
public class CommentSwipeActionsProvider {

  private static final String ACTION_NAME_REPLY = "CommentReply";
  private static final String ACTION_NAME_OPTIONS = "CommentOptions";
  private static final String ACTION_NAME_UPVOTE = "CommentUpvote";
  private static final String ACTION_NAME_DOWNVOTE = "CommentDownvote";

  private final VotingManager votingManager;
  private final UserSessionRepository userSessionRepository;
  private final OnLoginRequireListener onLoginRequireListener;
  private final SwipeActions commentSwipeActions;
  private final SwipeActionIconProvider swipeActionIconProvider;
  private OnReplySwipeActionListener onReplySwipeActionListener;

  public interface OnReplySwipeActionListener {
    void onReplySwipeAction(PostedOrInFlightContribution parentComment);
  }

  @Inject
  public CommentSwipeActionsProvider(
      VotingManager votingManager,
      UserSessionRepository userSessionRepository,
      OnLoginRequireListener loginRequireListener)
  {
    this.votingManager = votingManager;
    this.userSessionRepository = userSessionRepository;
    this.onLoginRequireListener = loginRequireListener;

    // Actions on both sides are aligned from left to right.
    commentSwipeActions = SwipeActions.builder()
        .startActions(SwipeActionsHolder.builder()
            .add(SwipeAction.create(ACTION_NAME_REPLY, R.color.list_item_swipe_reply, 1f))
            .add(SwipeAction.create(ACTION_NAME_OPTIONS, R.color.list_item_swipe_more_options, 1f))
            .build())
        .endActions(SwipeActionsHolder.builder()
            .add(SwipeAction.create(ACTION_NAME_UPVOTE, R.color.list_item_swipe_upvote, 1f))
            .add(SwipeAction.create(ACTION_NAME_DOWNVOTE, R.color.list_item_swipe_downvote, 1f))
            .build())
        .build();

    swipeActionIconProvider = createActionIconProvider();
  }

  public void setOnReplySwipeActionListener(OnReplySwipeActionListener listener) {
    onReplySwipeActionListener = listener;
  }

  public SwipeActions actions() {
    return commentSwipeActions;
  }

  public SwipeActionIconProvider iconProvider() {
    return swipeActionIconProvider;
  }

  public SwipeActionIconProvider createActionIconProvider() {
    return (imageView, oldAction, newAction) -> {
      switch (newAction.name()) {
        case ACTION_NAME_OPTIONS:
          resetIconRotation(imageView);
          imageView.setImageResource(R.drawable.ic_more_horiz_24dp);
          break;

        case ACTION_NAME_REPLY:
          resetIconRotation(imageView);
          imageView.setImageResource(R.drawable.ic_reply_24dp);
          break;

        case ACTION_NAME_UPVOTE:
          if (oldAction != null && ACTION_NAME_DOWNVOTE.equals(oldAction.name())) {
            imageView.setRotation(180);
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
    };
  }

  private void resetIconRotation(SwipeActionIconView imageView) {
    imageView.animate().cancel();
    imageView.setRotation(0);
  }

  public void performSwipeAction(SwipeAction swipeAction, PostedOrInFlightContribution comment, SwipeableLayout swipeableLayout) {
    if (!ACTION_NAME_OPTIONS.equals(swipeAction.name()) && !userSessionRepository.isUserLoggedIn()) {
      onLoginRequireListener.onLoginRequired();
      return;
    }

    boolean isUndoAction;

    switch (swipeAction.name()) {
      case ACTION_NAME_OPTIONS:
        Timber.i("TODO: %s", swipeAction.name());
        isUndoAction = false;
        break;

      case ACTION_NAME_REPLY:
        onReplySwipeActionListener.onReplySwipeAction(comment);
        isUndoAction = false;
        break;

      case ACTION_NAME_UPVOTE: {
        VoteDirection currentVoteDirection = votingManager.getPendingOrDefaultVote(comment, comment.voteDirection());
        VoteDirection newVoteDirection = currentVoteDirection == VoteDirection.UPVOTE ? VoteDirection.NO_VOTE : VoteDirection.UPVOTE;
        votingManager.voteWithAutoRetry(comment, newVoteDirection)
            .subscribeOn(Schedulers.io())
            .subscribe();

        isUndoAction = newVoteDirection == VoteDirection.NO_VOTE;
        break;
      }

      case ACTION_NAME_DOWNVOTE: {
        VoteDirection currentVoteDirection = votingManager.getPendingOrDefaultVote(comment, comment.voteDirection());
        VoteDirection newVoteDirection = currentVoteDirection == VoteDirection.DOWNVOTE ? VoteDirection.NO_VOTE : VoteDirection.DOWNVOTE;
        votingManager.voteWithAutoRetry(comment, newVoteDirection)
            .subscribeOn(Schedulers.io())
            .subscribe();

        isUndoAction = newVoteDirection == VoteDirection.NO_VOTE;
        break;
      }

      default:
        throw new UnsupportedOperationException("Unknown swipe action: " + swipeAction);
    }

    swipeableLayout.playRippleAnimation(swipeAction, isUndoAction ? RippleType.UNDO : RippleType.REGISTER);
  }
}
