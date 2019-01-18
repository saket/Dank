package me.saket.dank.ui.post

import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.rxkotlin.ofType
import me.saket.dank.ui.ReplayUntilScreenIsDestroyed
import me.saket.dank.ui.UiEvent
import javax.inject.Inject

typealias Ui = CreateNewPostActivity
typealias UiChange = (Ui) -> Unit

class CreateNewPostScreenController @Inject constructor(
    urlValidator: Lazy<UrlValidator>,
    outboxRepository: Lazy<PostOutboxRepository>
) : ObservableTransformer<UiEvent, UiChange> {

  override fun apply(events: Observable<UiEvent>): ObservableSource<UiChange> {
    val replayedEvents = ReplayUntilScreenIsDestroyed(events)
        .disposeWith(events.ofType())
        .replay()

    return Observable.mergeArray(
        autoResolvePostType(replayedEvents))
  }

  private fun autoResolvePostType(events: Observable<UiEvent>): Observable<UiChange> {
    return Observable.never()
  }
}
