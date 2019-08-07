package me.saket.dank.ui.preferences.gestures

import android.content.Context
import android.support.annotation.CheckResult
import io.reactivex.Observable
import me.saket.dank.R
import me.saket.dank.ui.preferences.gestures.submissions.GesturePreferencesSubmissionPreview
import me.saket.dank.ui.preferences.gestures.submissions.SubmissionSwipeActionsRepository
import me.saket.dank.ui.subreddit.SubmissionSwipeActions
import me.saket.dank.ui.subreddit.uimodels.SubredditUiConstructor
import me.saket.dank.utils.CombineLatestWithLog
import me.saket.dank.utils.CombineLatestWithLog.O
import me.saket.dank.walkthrough.SyntheticSubmission
import javax.inject.Inject

class GesturePreferencesUiConstructor @Inject constructor(
  private val uiConstructor: SubredditUiConstructor,
  private val swipeActionsRepository: SubmissionSwipeActionsRepository
) {
  @CheckResult
  fun stream(context: Context): Observable<GesturePreferencesScreenUiModel> {
    val startSwipeActionsObservable = swipeActionsRepository.startSwipeActions
      .map { swipeActions ->
        swipeActions.map {
          GesturePreferencesSwipeAction.UiModel(
            it,
            true,
            SubmissionSwipeActions.getSwipeActionIconRes(it)
          )
        }
      }

    val endSwipeActionsObservable = swipeActionsRepository.endSwipeActions
      .map { swipeActions ->
        swipeActions.map {
          GesturePreferencesSwipeAction.UiModel(
            it,
            false,
            SubmissionSwipeActions.getSwipeActionIconRes(it)
          )
        }
      }

    val submissionObservable = swipeActionsRepository.swipeActions
      .map { swipeActions ->
        GesturePreferencesSubmissionPreview.UiModel(
          uiConstructor.submissionUiModel(context, submission, 0, "Android", swipeActions)
        )
      }

    val startActionsHeader =
      GesturePreferencesSectionHeader.UiModel(context.getString(R.string.userprefs_gestures_group_swipe_start_to_end))
    val endActionsHeader =
      GesturePreferencesSectionHeader.UiModel(context.getString(R.string.userprefs_gestures_group_swipe_end_to_start))

    val startPlaceholder = GesturePreferencesSwipeActionPlaceholder.UiModel(true)
    val endPlaceholder = GesturePreferencesSwipeActionPlaceholder.UiModel(false)

    val hasUnusedStartSwipeActionsObservable = swipeActionsRepository.unusedSwipeActions(true)
      .map { it.isNotEmpty() }
    val hasUnusedEndSwipeActionsObservable = swipeActionsRepository.unusedSwipeActions(false)
      .map { it.isNotEmpty() }

    return CombineLatestWithLog.from(
      O.of("start swipe actions", startSwipeActionsObservable.distinctUntilChanged()),
      O.of("end swipe actions", endSwipeActionsObservable.distinctUntilChanged()),
      O.of("has unused start swipe actions", hasUnusedStartSwipeActionsObservable.distinctUntilChanged()),
      O.of("has unused end swipe actions", hasUnusedEndSwipeActionsObservable.distinctUntilChanged()),
      O.of("submission", submissionObservable.distinctUntilChanged())
    ) { startSwipeActions, endSwipeActions, hasUnusedStartSwipeActions, hasUnusedEndSwipeActions, submissionPreview ->
      GesturePreferencesScreenUiModel(
        sequenceOf(submissionPreview)
          .plus(startActionsHeader)
          .plus(startSwipeActions)
          .run { if (hasUnusedStartSwipeActions) plus(startPlaceholder) else this }
          .plus(endActionsHeader)
          .plus(endSwipeActions)
          .run { if (hasUnusedEndSwipeActions) plus(endPlaceholder) else this }
          .toList()
      )
    }
  }

  companion object {
    private val submission = SyntheticSubmission(0, title = "Customize gesture actions and swipe on this submission to check the result")
  }
}
