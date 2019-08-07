package me.saket.dank.ui.subreddit

import com.jakewharton.rxrelay2.PublishRelay
import dagger.Lazy
import me.saket.dank.R
import me.saket.dank.data.OnLoginRequireListener
import me.saket.dank.data.SwipeEvent
import me.saket.dank.ui.submission.BookmarksRepository
import me.saket.dank.ui.submission.events.ContributionVoteSwipeEvent
import me.saket.dank.ui.subreddit.events.SubmissionOpenInNewTabSwipeEvent
import me.saket.dank.ui.subreddit.events.SubmissionOptionSwipeEvent
import me.saket.dank.ui.user.UserSessionRepository
import me.saket.dank.utils.Animations
import me.saket.dank.vote.VotingManager
import me.saket.dank.walkthrough.SyntheticData
import me.saket.dank.widgets.swipe.SwipeAction
import me.saket.dank.widgets.swipe.SwipeActionIconView
import me.saket.dank.widgets.swipe.SwipeDirection
import me.saket.dank.widgets.swipe.SwipeTriggerRippleDrawable.RippleType
import me.saket.dank.widgets.swipe.SwipeableLayout
import net.dean.jraw.models.Submission
import net.dean.jraw.models.VoteDirection
import javax.inject.Inject

/**
 * Controls gesture actions on submissions.
 */
class SubmissionSwipeActionsProvider @Inject constructor(
  private val votingManager: Lazy<VotingManager>,
  private val bookmarksRepository: Lazy<BookmarksRepository>,
  private val userSessionRepository: Lazy<UserSessionRepository>,
  private val onLoginRequireListener: Lazy<OnLoginRequireListener>
) {
  val swipeEvents: PublishRelay<SwipeEvent> = PublishRelay.create<SwipeEvent>()

  fun showSwipeActionIcon(
    imageView: SwipeActionIconView,
    oldAction: SwipeAction?,
    newAction: SwipeAction,
    submission: Submission
  ) {
    when (newAction.labelRes()) {
      SubmissionSwipeAction.Options.displayNameRes -> {
        resetIconRotation(imageView)
        imageView.setImageResource(R.drawable.ic_more_horiz_24dp)
      }

      SubmissionSwipeAction.NewTab.displayNameRes -> {
        resetIconRotation(imageView)
        imageView.setImageResource(R.drawable.ic_open_in_new_tab_24dp)
      }

      SubmissionSwipeAction.Save.displayNameRes -> {
        resetIconRotation(imageView)
        imageView.setImageResource(if (bookmarksRepository.get().isSaved(submission)) R.drawable.ic_unsave_24dp else R.drawable.ic_save_24dp)
      }

      SubmissionSwipeAction.Upvote.displayNameRes -> if (oldAction != null && SubmissionSwipeAction.Downvote.displayNameRes == oldAction.labelRes()) {
        imageView.setImageResource(R.drawable.ic_arrow_upward_24dp)
        imageView.rotation =
            180f   // We want to play a circular animation if the user keeps switching between upvote and downvote.
        imageView.animate().rotationBy(180f).setInterpolator(Animations.INTERPOLATOR).setDuration(200).start()
      } else {
        resetIconRotation(imageView)
        imageView.setImageResource(R.drawable.ic_arrow_upward_24dp)
      }

      SubmissionSwipeAction.Downvote.displayNameRes -> if (oldAction != null && SubmissionSwipeAction.Upvote.displayNameRes == oldAction.labelRes()) {
        imageView.setImageResource(R.drawable.ic_arrow_downward_24dp)
        imageView.rotation = 180f
        imageView.animate().rotationBy(180f).setInterpolator(Animations.INTERPOLATOR).setDuration(200).start()
      } else {
        resetIconRotation(imageView)
        imageView.setImageResource(R.drawable.ic_arrow_downward_24dp)
      }

      else -> throw UnsupportedOperationException("Unknown swipe action: $newAction")
    }
  }

  fun performSwipeAction(
    swipeAction: SwipeAction,
    submission: Submission,
    swipeableLayout: SwipeableLayout,
    swipeDirection: SwipeDirection
  ) {
    if (needsLogin(swipeAction, submission)) {
      // Delay because showing LoginActivity for the first time stutters SwipeableLayout's reset animation.
      swipeableLayout.postDelayed(
        { onLoginRequireListener.get().onLoginRequired() },
        SwipeableLayout.ANIMATION_DURATION_FOR_SETTLING_BACK_TO_POSITION
      )
      return
    }

    val isUndoAction: Boolean

    when (swipeAction.labelRes()) {
      SubmissionSwipeAction.Options.displayNameRes -> {
        swipeEvents.accept(SubmissionOptionSwipeEvent.create(submission, swipeableLayout))
        isUndoAction = false
      }

      SubmissionSwipeAction.NewTab.displayNameRes -> {
        swipeEvents.accept(SubmissionOpenInNewTabSwipeEvent.create(submission, swipeableLayout))
        isUndoAction = false
      }

      SubmissionSwipeAction.Save.displayNameRes -> {
        isUndoAction = if (bookmarksRepository.get().isSaved(submission)) {
          bookmarksRepository.get().markAsUnsaved(submission)
          true
        } else {
          bookmarksRepository.get().markAsSaved(submission)
          false
        }
      }

      SubmissionSwipeAction.Upvote.displayNameRes -> {
        val currentVoteDirection = votingManager.get().getPendingOrDefaultVote(submission, submission.vote)
        val newVoteDirection = if (currentVoteDirection == VoteDirection.UP) VoteDirection.NONE else VoteDirection.UP
        swipeEvents.accept(ContributionVoteSwipeEvent.create(submission, newVoteDirection))
        isUndoAction = newVoteDirection == VoteDirection.NONE
      }

      SubmissionSwipeAction.Downvote.displayNameRes -> {
        val currentVoteDirection = votingManager.get().getPendingOrDefaultVote(submission, submission.vote)
        val newVoteDirection =
          if (currentVoteDirection == VoteDirection.DOWN) VoteDirection.NONE else VoteDirection.DOWN
        swipeEvents.accept(ContributionVoteSwipeEvent.create(submission, newVoteDirection))
        isUndoAction = newVoteDirection == VoteDirection.NONE
      }

      else -> throw UnsupportedOperationException("Unknown swipe action: $swipeAction")
    }

    swipeableLayout.playRippleAnimation(
      swipeAction,
      if (isUndoAction) RippleType.UNDO else RippleType.REGISTER,
      swipeDirection
    )
  }

  private fun needsLogin(swipeAction: SwipeAction, submission: Submission): Boolean {
    if (SyntheticData.isSynthetic(submission)) {
      return false
    }

    return when (swipeAction.labelRes()) {
      SubmissionSwipeAction.Options.displayNameRes,
      SubmissionSwipeAction.NewTab.displayNameRes -> {
        false
      }

      SubmissionSwipeAction.Save.displayNameRes,
      SubmissionSwipeAction.Upvote.displayNameRes,
      SubmissionSwipeAction.Downvote.displayNameRes -> {
        !userSessionRepository.get().isUserLoggedIn
      }

      else -> throw UnsupportedOperationException("Unknown swipe action: $swipeAction")
    }
  }

  private fun resetIconRotation(imageView: SwipeActionIconView) {
    imageView.animate().cancel()
    imageView.rotation = 0f
  }
}
