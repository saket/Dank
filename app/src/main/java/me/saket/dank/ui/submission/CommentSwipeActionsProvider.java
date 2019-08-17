package me.saket.dank.ui.submission;

import androidx.annotation.StringRes;

import com.jakewharton.rxrelay2.PublishRelay;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.VoteDirection;

import javax.inject.Inject;

import dagger.Lazy;
import me.saket.dank.R;
import me.saket.dank.data.OnLoginRequireListener;
import me.saket.dank.data.SwipeEvent;
import me.saket.dank.ui.submission.events.CommentOptionSwipeEvent;
import me.saket.dank.ui.submission.events.ContributionVoteSwipeEvent;
import me.saket.dank.ui.submission.events.InlineReplyRequestEvent;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.utils.Animations;
import me.saket.dank.vote.VotingManager;
import me.saket.dank.walkthrough.SyntheticData;
import me.saket.dank.widgets.swipe.SwipeAction;
import me.saket.dank.widgets.swipe.SwipeActionIconView;
import me.saket.dank.widgets.swipe.SwipeActions;
import me.saket.dank.widgets.swipe.SwipeActionsHolder;
import me.saket.dank.widgets.swipe.SwipeDirection;
import me.saket.dank.widgets.swipe.SwipeTriggerRippleDrawable.RippleType;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.SwipeableLayout.SwipeActionIconProvider;

/**
 * Controls gesture actions on comments and submission details.
 */
public class CommentSwipeActionsProvider {

  private static final @StringRes int ACTION_NAME_REPLY = R.string.submission_comment_swipe_action_reply;
  private static final @StringRes int ACTION_NAME_OPTIONS = R.string.submission_comment_swipe_action_options;
  private static final @StringRes int ACTION_NAME_UPVOTE = R.string.submission_comment_swipe_action_upvote;
  private static final @StringRes int ACTION_NAME_DOWNVOTE = R.string.submission_comment_swipe_action_downvote;

  private final Lazy<VotingManager> votingManager;
  private final Lazy<UserSessionRepository> userSessionRepository;
  private final Lazy<OnLoginRequireListener> onLoginRequireListener;
  private final SwipeActions commentSwipeActions;
  private final SwipeActionIconProvider swipeActionIconProvider;
  public final PublishRelay<SwipeEvent> swipeEvents = PublishRelay.create();

  @Inject
  public CommentSwipeActionsProvider(
      Lazy<VotingManager> votingManager,
      Lazy<UserSessionRepository> userSessionRepository,
      Lazy<OnLoginRequireListener> loginRequireListener)
  {
    this.votingManager = votingManager;
    this.userSessionRepository = userSessionRepository;
    this.onLoginRequireListener = loginRequireListener;

    // Actions on both sides are aligned from left to right.
    commentSwipeActions = SwipeActions.builder()
        .startActions(SwipeActionsHolder.builder()
            .add(SwipeAction.create(ACTION_NAME_REPLY, R.color.list_item_swipe_reply, 0.5f))
            .add(SwipeAction.create(ACTION_NAME_OPTIONS, R.color.list_item_swipe_more_options, 0.5f))
            .build())
        .endActions(SwipeActionsHolder.builder()
            .add(SwipeAction.create(ACTION_NAME_UPVOTE, R.color.list_item_swipe_upvote, 0.4f))
            .add(SwipeAction.create(ACTION_NAME_DOWNVOTE, R.color.list_item_swipe_downvote, 0.6f))
            .build())
        .build();

    swipeActionIconProvider = createActionIconProvider();
  }

  public SwipeActions actions() {
    return commentSwipeActions;
  }

  public SwipeActionIconProvider iconProvider() {
    return swipeActionIconProvider;
  }

  public SwipeActionIconProvider createActionIconProvider() {
    return (imageView, oldAction, newAction) -> {
      switch (newAction.labelRes()) {
        case ACTION_NAME_OPTIONS:
          resetIconRotation(imageView);
          imageView.setImageResource(R.drawable.ic_more_horiz_24dp);
          break;

        case ACTION_NAME_REPLY:
          resetIconRotation(imageView);
          imageView.setImageResource(R.drawable.ic_reply_24dp);
          break;

        case ACTION_NAME_UPVOTE:
          if (oldAction != null && ACTION_NAME_DOWNVOTE == oldAction.labelRes()) {
            imageView.setRotation(180);
            imageView.animate().rotationBy(180).setInterpolator(Animations.INTERPOLATOR).setDuration(200).start();
          } else {
            resetIconRotation(imageView);
            imageView.setImageResource(R.drawable.ic_arrow_upward_24dp);
          }
          break;

        case ACTION_NAME_DOWNVOTE:
          if (oldAction != null && ACTION_NAME_UPVOTE == oldAction.labelRes()) {
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

  public void performSwipeAction(SwipeAction swipeAction, Comment comment, SwipeableLayout swipeableLayout, SwipeDirection swipeDirection) {
    if (needsLogin(swipeAction, comment)) {
      // Delay because showing LoginActivity for the first time stutters SwipeableLayout's reset animation.
      swipeableLayout.postDelayed(
          () -> onLoginRequireListener.get().onLoginRequired(),
          SwipeableLayout.ANIMATION_DURATION_FOR_SETTLING_BACK_TO_POSITION);
      return;
    }

    boolean isUndoAction;

    switch (swipeAction.labelRes()) {
      case ACTION_NAME_OPTIONS:
        swipeEvents.accept(new CommentOptionSwipeEvent(comment, swipeableLayout));
        isUndoAction = false;
        break;

      case ACTION_NAME_REPLY:
        swipeEvents.accept(InlineReplyRequestEvent.create(comment));
        isUndoAction = false;
        break;

      case ACTION_NAME_UPVOTE: {
        VoteDirection currentVoteDirection = votingManager.get().getPendingOrDefaultVote(comment, comment.getVote());
        VoteDirection newVoteDirection = currentVoteDirection == VoteDirection.UP ? VoteDirection.NONE : VoteDirection.UP;
        swipeEvents.accept(ContributionVoteSwipeEvent.create(comment, newVoteDirection));
        isUndoAction = newVoteDirection == VoteDirection.NONE;
        break;
      }

      case ACTION_NAME_DOWNVOTE: {
        VoteDirection currentVoteDirection = votingManager.get().getPendingOrDefaultVote(comment, comment.getVote());
        VoteDirection newVoteDirection = currentVoteDirection == VoteDirection.DOWN ? VoteDirection.NONE : VoteDirection.DOWN;
        swipeEvents.accept(ContributionVoteSwipeEvent.create(comment, newVoteDirection));
        isUndoAction = newVoteDirection == VoteDirection.NONE;
        break;
      }

      default:
        throw new UnsupportedOperationException("Unknown swipe action: " + swipeAction);
    }

    swipeableLayout.playRippleAnimation(swipeAction, isUndoAction ? RippleType.UNDO : RippleType.REGISTER, swipeDirection);
  }

  private boolean needsLogin(SwipeAction swipeAction, Comment comment) {
    if (SyntheticData.Companion.isSynthetic(comment)) {
     return false;
    }

    switch (swipeAction.labelRes()) {
      case ACTION_NAME_OPTIONS:
        return false;

      case ACTION_NAME_REPLY:
      case ACTION_NAME_UPVOTE:
      case ACTION_NAME_DOWNVOTE:
        return !userSessionRepository.get().isUserLoggedIn();

      default:
        throw new UnsupportedOperationException("Unknown swipe action: " + swipeAction);
    }
  }
}
