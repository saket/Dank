package me.saket.dank.ui.submission;

import android.graphics.Point;
import android.view.Gravity;

import com.jakewharton.rxrelay2.PublishRelay;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.VoteDirection;

import javax.inject.Inject;

import dagger.Lazy;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.R;
import me.saket.dank.data.OnLoginRequireListener;
import me.saket.dank.data.VotingManager;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.swipe.SwipeAction;
import me.saket.dank.widgets.swipe.SwipeActionIconView;
import me.saket.dank.widgets.swipe.SwipeActions;
import me.saket.dank.widgets.swipe.SwipeActionsHolder;
import me.saket.dank.widgets.swipe.SwipeTriggerRippleDrawable.RippleType;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.SwipeableLayout.SwipeActionIconProvider;

/**
 * Controls gesture actions on comments and submission details.
 */
public class CommentSwipeActionsProvider {

  private static final String ACTION_NAME_REPLY = "CommentReply";
  private static final String ACTION_NAME_OPTIONS = "CommentOptions";
  private static final String ACTION_NAME_UPVOTE = "CommentUpvote";
  private static final String ACTION_NAME_DOWNVOTE = "CommentDownvote";

  private final Lazy<VotingManager> votingManager;
  private final Lazy<UserSessionRepository> userSessionRepository;
  private final Lazy<OnLoginRequireListener> onLoginRequireListener;
  private final SwipeActions commentSwipeActions;
  private final SwipeActionIconProvider swipeActionIconProvider;

  public final PublishRelay<Comment> replySwipeActions = PublishRelay.create();

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
            .add(SwipeAction.create(ACTION_NAME_REPLY, R.color.list_item_swipe_reply, 0.3f))
            .add(SwipeAction.create(ACTION_NAME_OPTIONS, R.color.list_item_swipe_more_options, 0.7f))
            .build())
        .endActions(SwipeActionsHolder.builder()
            .add(SwipeAction.create(ACTION_NAME_UPVOTE, R.color.list_item_swipe_upvote, 0.3f))
            .add(SwipeAction.create(ACTION_NAME_DOWNVOTE, R.color.list_item_swipe_downvote, 0.7f))
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

  public void performSwipeAction(SwipeAction swipeAction, Comment comment, SwipeableLayout swipeableLayout) {
    if (!ACTION_NAME_OPTIONS.equals(swipeAction.name()) && !userSessionRepository.get().isUserLoggedIn()) {
      onLoginRequireListener.get().onLoginRequired();
      return;
    }

    boolean isUndoAction;

    switch (swipeAction.name()) {
      case ACTION_NAME_OPTIONS:
        Point sheetLocation = Views.locationOnScreen(swipeableLayout);
        Point menuLocation = new Point(0, sheetLocation.y);
        menuLocation.offset(  // Align with comment body.
            swipeableLayout.getResources().getDimensionPixelSize(R.dimen.submission_comment_horiz_padding),
            swipeableLayout.getResources().getDimensionPixelSize(R.dimen.submission_comment_top_padding));

        CommentOptionsPopup optionsPopup = new CommentOptionsPopup(swipeableLayout.getContext(), comment);
        optionsPopup.showAtLocation(swipeableLayout, Gravity.TOP | Gravity.START, menuLocation);
        isUndoAction = false;
        break;

      case ACTION_NAME_REPLY:
        replySwipeActions.accept(comment);
        isUndoAction = false;
        break;

      case ACTION_NAME_UPVOTE: {
        VoteDirection currentVoteDirection = votingManager.get().getPendingOrDefaultVote(comment, comment.getVote());
        VoteDirection newVoteDirection = currentVoteDirection == VoteDirection.UPVOTE ? VoteDirection.NO_VOTE : VoteDirection.UPVOTE;
        votingManager.get().voteWithAutoRetry(comment, newVoteDirection)
            .subscribeOn(Schedulers.io())
            .subscribe();

        isUndoAction = newVoteDirection == VoteDirection.NO_VOTE;
        break;
      }

      case ACTION_NAME_DOWNVOTE: {
        VoteDirection currentVoteDirection = votingManager.get().getPendingOrDefaultVote(comment, comment.getVote());
        VoteDirection newVoteDirection = currentVoteDirection == VoteDirection.DOWNVOTE ? VoteDirection.NO_VOTE : VoteDirection.DOWNVOTE;
        votingManager.get().voteWithAutoRetry(comment, newVoteDirection)
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
