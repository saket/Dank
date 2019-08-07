package me.saket.dank.ui.preferences.gestures.submissions

import com.f2prateek.rx.preferences2.Preference
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers.io
import me.saket.dank.ui.subreddit.SubmissionSwipeAction
import me.saket.dank.ui.subreddit.SubmissionSwipeActions
import me.saket.dank.utils.CombineLatestWithLog
import me.saket.dank.utils.CombineLatestWithLog.O
import me.saket.dank.widgets.swipe.SwipeActions
import me.saket.dank.widgets.swipe.SwipeActionsHolder
import javax.inject.Inject
import javax.inject.Named

class SubmissionSwipeActionsRepository @Inject constructor(
  @Named("submission_start_swipe_actions") val startSwipeActionsPref: Preference<List<SubmissionSwipeAction>>,
  @Named("submission_end_swipe_actions") val endSwipeActionsPref: Preference<List<SubmissionSwipeAction>>
) {
  val startSwipeActions: Observable<List<SubmissionSwipeAction>> = startSwipeActionsPref.asObservable()
  val endSwipeActions: Observable<List<SubmissionSwipeAction>> = endSwipeActionsPref.asObservable()

  val defaultSwipeActions: SwipeActions by lazy {
    SwipeActions.builder()
      .startActions(buildSwipeActions(startSwipeActionsPref.defaultValue()))
      .endActions(buildSwipeActions(endSwipeActionsPref.defaultValue()))
      .build()
  }

  val swipeActions: Observable<SwipeActions> = CombineLatestWithLog.from(
    O.of("start swipe actions", startSwipeActions.observeOn(io()).map(::buildSwipeActions).distinctUntilChanged()),
    O.of("end swipe actions", endSwipeActions.observeOn(io()).map(::buildSwipeActions).distinctUntilChanged())
  ) { startActions, endActions ->
    SwipeActions.builder()
      .startActions(startActions)
      .endActions(endActions)
      .build()
  }

  private fun buildSwipeActions(submissionSwipeActions: List<SubmissionSwipeAction>): SwipeActionsHolder {
    val builder = SwipeActionsHolder.builder()
    for (swipeAction in submissionSwipeActions) {
      builder.add(SubmissionSwipeActions.getAction(swipeAction))
    }
    return builder.build()
  }

  fun unusedSwipeActions(forStartPosition: Boolean): Observable<List<SubmissionSwipeAction>> =
    getPreference(forStartPosition).asObservable()
      .subscribeOn(io())
      .map { SubmissionSwipeAction.values().asList().minus(it) }

  fun addSwipeAction(swipeAction: SubmissionSwipeAction, forStartPosition: Boolean) {
    val preference = getPreference(forStartPosition)
    preference.set(preference.get() + swipeAction)
  }

  fun removeSwipeAction(swipeAction: SubmissionSwipeAction, forStartPosition: Boolean) {
    val preference = getPreference(forStartPosition)
    preference.set(preference.get() - swipeAction)
  }

  fun setSwipeActions(swipeActions: List<SubmissionSwipeAction>, forStartPosition: Boolean) {
    getPreference(forStartPosition).set(swipeActions)
  }

  private fun getPreference(forStartPosition: Boolean) =
    if (forStartPosition) startSwipeActionsPref else endSwipeActionsPref
}
