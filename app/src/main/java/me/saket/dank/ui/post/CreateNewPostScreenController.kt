package me.saket.dank.ui.post

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import me.saket.dank.ui.UiEvent
import javax.inject.Inject

typealias Ui = CreateNewPostActivity
typealias UiChange = (Ui) -> Unit

class CreateNewPostScreenController @Inject constructor(): ObservableTransformer<UiEvent, UiChange> {

  override fun apply(events: Observable<UiEvent>): ObservableSource<UiChange> {
    return Observable.never()
  }
}
