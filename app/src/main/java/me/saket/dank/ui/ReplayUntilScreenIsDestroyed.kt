package me.saket.dank.ui

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign

/**
 * Helper class for sharing Ui event stream using replay().autoConnect()
 * because it's better than share() and replay().refCount().
 *
 * share() does not work very well in cases where events get emitted before
 * all subscribers are able to subscribe to share().
 *
 * replay().refCount() can dispose and subscribe multiple times, potentially
 * leading to race conditions when threading is involved.
 *
 * Copied from:
 * https://github.com/simpledotorg/simple-android/blob/master/app/src/main/java/org/simple/clinic/ReplayUntilScreenIsDestroyed.kt
 */
class ReplayUntilScreenIsDestroyed(private val events: Observable<UiEvent>) {

  private var transforms: List<ObservableTransformer<UiEvent, UiEvent>> = emptyList()

  private val disposables = CompositeDisposable()
  private var disposalSetup = false

  fun compose(transformer: ObservableTransformer<UiEvent, UiEvent>): ReplayUntilScreenIsDestroyed {
    transforms += transformer
    return this
  }

  fun disposeWith(terminalStream: Observable<ScreenDestroyed>): ReplayUntilScreenIsDestroyed {
    disposalSetup = true
    disposables.add(
        terminalStream
            .subscribe { disposables.clear() })
    return this
  }

  fun replay(): Observable<UiEvent> {
    if (disposalSetup.not()) {
      throw AssertionError()
    }

    val replayedEvents = events
        .replay()
        .autoConnect(1) { disposables += it }

    return transforms
        .fold(replayedEvents) { events, transform ->
          events.compose(transform)
              .replay()
              .autoConnect(1) { disposables += it }
        }
  }
}
