package me.saket.dank.ui.subreddit

import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers.io
import me.saket.dank.data.InboxRepository
import me.saket.dank.reddit.Reddit
import me.saket.dank.ui.ReplayUntilScreenIsDestroyed
import me.saket.dank.ui.UiEvent
import me.saket.dank.ui.submission.AuditedCommentSort
import me.saket.dank.ui.submission.AuditedCommentSort.SelectedBy
import me.saket.dank.ui.submission.SubmissionRepository
import me.saket.dank.ui.subreddit.events.SubredditScreenCreateEvent
import me.saket.dank.ui.subreddit.events.SubredditSubmissionClickEvent
import me.saket.dank.ui.user.UserSessionRepository
import me.saket.dank.ui.user.messages.InboxFolder
import me.saket.dank.utils.DankSubmissionRequest
import me.saket.dank.utils.Optional
import me.saket.dank.walkthrough.SubmissionGestureWalkthroughProceedEvent
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout.PageState.COLLAPSED
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout.PageState.COLLAPSING
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout.PageState.EXPANDED
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout.PageState.EXPANDING
import java.util.concurrent.TimeUnit
import javax.inject.Inject

typealias Ui = SubredditUi
typealias UiChange = (Ui) -> Unit

class SubredditController @Inject constructor(
    private val inboxRepository: Lazy<InboxRepository>,
    private val userSessionRepository: Lazy<UserSessionRepository>,
    private val submissionRepository: Lazy<SubmissionRepository>
) : ObservableTransformer<UiEvent, UiChange> {

  override fun apply(events: Observable<UiEvent>): ObservableSource<UiChange> {
    val replayedEvents = ReplayUntilScreenIsDestroyed(events)
        .compose(syntheticSubmissionsForGesturesWalkthrough())
        .disposeWith(events.ofType())
        .replay()

    return Observable.mergeArray(
        unreadMessageIconChanges(replayedEvents),
        submissionExpansions(replayedEvents),
        toggleFabVisibility(replayedEvents),
        keepActivityResizableWhileSubmissionIsOpen(replayedEvents))
  }

  private fun syntheticSubmissionsForGesturesWalkthrough() = ObservableTransformer<UiEvent, UiEvent> { events ->
    val openWalkthroughSubmission = events
        .ofType<SubmissionGestureWalkthroughProceedEvent>()
        .flatMapSingle { event ->
          submissionRepository.get()
              .syntheticSubmissionForGesturesWalkthrough()
              .subscribeOn(io())
              .map { event.toSubmissionClickEvent(it.submission) }
        }
    events.mergeWith(openWalkthroughSubmission)
  }

  private fun unreadMessageIconChanges(events: Observable<UiEvent>): Observable<UiChange> {
    return events
        .ofType<SubredditScreenCreateEvent>()
        .switchMap { userSessionRepository.get().streamSessions() }
        .filter { it.isPresent }
        .switchMap {
          val unreadCountsFromInbox = inboxRepository.get()
              .messages(InboxFolder.UNREAD)
              .map { unreads -> unreads.size }

          unreadCountsFromInbox
              .subscribeOn(io())
              .map { unreadCount -> unreadCount > 0 }
              .map { hasUnreads ->
                when {
                  hasUnreads -> SubredditUserProfileIconType.USER_PROFILE_WITH_UNREAD_MESSAGES
                  else -> SubredditUserProfileIconType.USER_PROFILE
                }
              }
              .map { icon -> { ui: Ui -> ui.setToolbarUserProfileIcon(icon) } }
        }
  }

  private fun submissionExpansions(events: Observable<UiEvent>): Observable<UiChange> {
    val subredditNameChanges = events
        .ofType<SubredditChanged>()
        .map { it.subredditName }

    val populations = events
        .ofType<SubredditSubmissionClickEvent>()
        .withLatestFrom(subredditNameChanges)
        .map<UiChange> { (clickEvent, subredditName) ->
          val submission = clickEvent.submission()

          val auditedSort = Optional.ofNullable(submission.suggestedSort)
              .map { sort -> AuditedCommentSort.create(sort, SelectedBy.SUBMISSION_SUGGESTED) }
              .orElse(AuditedCommentSort.create(Reddit.DEFAULT_COMMENT_SORT, SelectedBy.DEFAULT))

          val submissionRequest = DankSubmissionRequest.builder(submission.id)
              .commentSort(auditedSort)
              .build()

          return@map { ui: Ui -> ui.populateSubmission(submission, submissionRequest, subredditName) }
        }

    val expansions = events
        .ofType<SubredditSubmissionClickEvent>()
        // This delay ensures that they submission is almost
        // ready to be shown and will not stutter while expanding.
        .delay(100, TimeUnit.MILLISECONDS)
        .map { event -> { ui: Ui -> ui.expandSubmissionRow(event.itemView(), event.itemId()) } }

    return populations.mergeWith(expansions)
  }

  private fun toggleFabVisibility(events: Observable<UiEvent>): Observable<UiChange> {
    val show = true
    val hide = false

    val toggleOnListScroll = events
        .ofType<SubredditListScrolled>()
        .filter { it.isListRefreshEvent.not() }
        .map { it.dy < 0 }
        .distinctUntilChanged()

    val showOnStart = Observable.just(show)

    val toggleOnSubmissionExpansion = events
        .ofType<SubmissionPageStateChanged>()
        .map {
          when (it.pageState) {
            COLLAPSING -> show
            COLLAPSED -> show
            EXPANDING -> hide
            EXPANDED -> hide
          }
        }

    return Observable
        .merge(toggleOnListScroll, showOnStart, toggleOnSubmissionExpansion)
        .map { upwardScroll ->
          { ui: Ui -> ui.setFabVisible(upwardScroll) }
        }
  }

  /**
   * FYI: not sure why, but for this to work, the windowSoftInputMode needs
   * to be set to "adjustNothing" by default in Manifest.
   */
  private fun keepActivityResizableWhileSubmissionIsOpen(events: Observable<UiEvent>): Observable<UiChange> {
    return events
        .ofType<SubmissionPageStateChanged>()
        .map {
          when (it.pageState) {
            COLLAPSING -> WindowSoftInputMode.ADJUST_NOTHING
            COLLAPSED -> WindowSoftInputMode.ADJUST_NOTHING
            EXPANDING -> WindowSoftInputMode.ADJUST_RESIZE
            EXPANDED -> WindowSoftInputMode.ADJUST_RESIZE
          }
        }
        .distinctUntilChanged()
        .map { { ui: Ui -> ui.setWindowSoftInputMode(it) } }
  }
}
