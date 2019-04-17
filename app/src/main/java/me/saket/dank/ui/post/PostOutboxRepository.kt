package me.saket.dank.ui.post

import io.reactivex.Completable
import javax.inject.Inject

class PostOutboxRepository @Inject constructor() {

  fun submit(postToSubmit: PostToSubmit): Completable {
    TODO()
  }
}
