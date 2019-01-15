package me.saket.dank.ui.subreddit

import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers.io
import me.saket.dank.data.InboxRepository
import me.saket.dank.reddit.Reddit
import me.saket.dank.ui.UiChange
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
import me.saket.dank.utils.Pair
import me.saket.dank.walkthrough.SubmissionGestureWalkthroughProceedEvent
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SubredditController @Inject
constructor(
    private val inboxRepository: Lazy<InboxRepository>,
    private val userSessionRepository: Lazy<UserSessionRepository>,
    private val submissionRepository: Lazy<SubmissionRepository>
) : ObservableTransformer<UiEvent, UiChange<SubredditUi>> {

  override fun apply(events: Observable<UiEvent>): ObservableSource<UiChange<SubredditUi>> {
    val replayedEvents = events.replay().refCount()

    val transformedEvents = replayedEvents
        .mergeWith(syntheticSubmissionsForGesturesWalkthrough(replayedEvents))

    return Observable.mergeArray(
        unreadMessageIconChanges(transformedEvents),
        submissionExpansions(transformedEvents))
  }

  private fun syntheticSubmissionsForGesturesWalkthrough(events: Observable<UiEvent>): Observable<UiEvent> {
    return events
        .ofType(SubmissionGestureWalkthroughProceedEvent::class.java)
        .flatMapSingle { event ->
          submissionRepository.get()
              .syntheticSubmissionForGesturesWalkthrough()
              .subscribeOn(io())
              .observeOn(mainThread())
              .map { submissionData -> event.toSubmissionClickEvent(submissionData.submission) }
              .onErrorResumeNext { e ->
                e.printStackTrace()
                Single.never()
              }
        }
  }

  private fun unreadMessageIconChanges(events: Observable<UiEvent>): Observable<UiChange<SubredditUi>> {
    return events
        .ofType(SubredditScreenCreateEvent::class.java)
        .switchMap { userSessionRepository.get().streamSessions() }
        .filter { session -> session.isPresent }
        .switchMap {
          Timber.i("Fetching user profile for updating unread message icon")

          //Observable<Integer> unreadCountsFromAccount = userProfileRepository.get()
          //    .loggedInUserAccounts()
          //    .map(account -> account.getInboxCount());

          val unreadCountsFromInbox = inboxRepository.get()
              .messages(InboxFolder.UNREAD)
              .map { unreads -> unreads.size }

          unreadCountsFromInbox
              //.mergeWith(unreadCountsFromAccount)
              .subscribeOn(io())
              .observeOn(mainThread())
              .map { unreadCount -> unreadCount > 0 }
              .map { hasUnreads ->
                if (hasUnreads)
                  SubredditUserProfileIconType.USER_PROFILE_WITH_UNREAD_MESSAGES
                else
                  SubredditUserProfileIconType.USER_PROFILE
              }
              .map { iconType -> UiChange { ui: SubredditUi -> ui.setToolbarUserProfileIcon(iconType) } }
        }
  }

  private fun submissionExpansions(events: Observable<UiEvent>): Observable<UiChange<SubredditUi>> {
    val subredditNameChanges = events
        .ofType(SubredditChangeEvent::class.java)
        .map { event -> event.subredditName() }

    val populations = events
        .ofType(SubredditSubmissionClickEvent::class.java)
        .withLatestFrom<String, Pair<SubredditSubmissionClickEvent, String>>(subredditNameChanges, BiFunction<SubredditSubmissionClickEvent, String, Pair<SubredditSubmissionClickEvent, String>> { first, second -> Pair.create(first, second) })
        .map<UiChange<SubredditUi>> { pair ->
          val clickEvent = pair.first()
          val currentSubredditName = pair.second()
          val submission = clickEvent.submission()

          val auditedSort = Optional.ofNullable(submission.suggestedSort)
              .map { sort -> AuditedCommentSort.create(sort, SelectedBy.SUBMISSION_SUGGESTED) }
              .orElse(AuditedCommentSort.create(Reddit.DEFAULT_COMMENT_SORT, SelectedBy.DEFAULT))

          val submissionRequest = DankSubmissionRequest.builder(submission.id)
              .commentSort(auditedSort)
              .build()

          UiChange { ui -> ui.populateSubmission(submission, submissionRequest, currentSubredditName) }
        }

    val expansions = events
        .ofType(SubredditSubmissionClickEvent::class.java)
        // This delay ensures that they submission is almost
        // ready to be shown and will not stutter while expanding.
        .delay(100, TimeUnit.MILLISECONDS, mainThread())
        .map { event -> UiChange<SubredditUi> { ui -> ui.expandSubmissionRow(event.itemView(), event.itemId()) } }

    return populations.mergeWith(expansions)
  }
}
