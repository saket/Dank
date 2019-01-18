package me.saket.dank.ui.post

import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.withLatestFrom
import me.saket.dank.ui.ReplayUntilScreenIsDestroyed
import me.saket.dank.ui.UiEvent
import me.saket.dank.ui.post.NewPostKind.IMAGE
import me.saket.dank.ui.post.NewPostKind.LINK
import me.saket.dank.ui.post.NewPostKind.SELF_TEXT
import me.saket.dank.ui.post.UrlValidator.Result.Invalid
import me.saket.dank.ui.post.UrlValidator.Result.Valid
import javax.inject.Inject

typealias Ui = CreateNewPostActivity
typealias UiChange = (Ui) -> Unit

class CreateNewPostScreenController @Inject constructor(
    private val urlValidator: Lazy<UrlValidator>,
    private val outboxRepository: Lazy<PostOutboxRepository>
) : ObservableTransformer<UiEvent, UiChange> {

  override fun apply(events: Observable<UiEvent>): ObservableSource<UiChange> {
    val replayedEvents = ReplayUntilScreenIsDestroyed(events)
        .disposeWith(events.ofType())
        .replay()

    return Observable.mergeArray(
        autoResolvePostType(replayedEvents),
        focusTitleOnBackPressInEmptyBody(replayedEvents),
        toggleSubmitButton(replayedEvents))
  }

  private fun autoResolvePostType(events: Observable<UiEvent>): Observable<UiChange> {
    val bodyTextChanges = events
        .ofType<NewPostBodyTextChanged>()
        .map { it.body }

    val imageSelectionChanges = events
        .ofType<NewPostImageSelectionUpdated>()
        .map { it.images }

    return Observables.combineLatest(bodyTextChanges, imageSelectionChanges)
        .map { (body, images) ->
          if (images.isNotEmpty()) {
            IMAGE
          } else {
            val urlValidation = urlValidator.get().validate(body)
            when (urlValidation) {
              Valid -> LINK
              Invalid -> SELF_TEXT
            }
          }
        }
        .map { { ui: Ui -> ui.setDetectedPostKind(it) } }
  }

  private fun focusTitleOnBackPressInEmptyBody(events: Observable<UiEvent>): Observable<UiChange> {
    val bodyTextChanges = events
        .ofType<NewPostBodyTextChanged>()
        .map { it.body }

    return events
        .ofType<NewPostBodyBackspaceClicked>()
        .withLatestFrom(bodyTextChanges)
        .filter { (_, body) -> body.isEmpty() }
        .map { { ui: Ui -> ui.requestFocusOnTitleField() } }
  }

  private fun toggleSubmitButton(events: Observable<UiEvent>): Observable<UiChange> {
    val titleTextChanges = events
        .ofType<NewPostTitleTextChanged>()
        .map { it.title }

    val subredditSelections = events
        .ofType<NewPostSubredditSelected>()
        .map { it.subredditName }

    return Observables.combineLatest(titleTextChanges, subredditSelections)
        .map { (title, subredditName) -> title.isNotBlank() && subredditName.isNotBlank() }
        .startWith(false)
        .map { { ui: Ui -> ui.setSubmitButtonEnabled(it) } }
  }
}
