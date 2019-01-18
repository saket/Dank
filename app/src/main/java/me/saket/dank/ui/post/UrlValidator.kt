package me.saket.dank.ui.post

import javax.inject.Inject

class UrlValidator @Inject constructor() {

  sealed class Result {
    object Valid : Result()
    object Invalid : Result()
  }

  // TODO: this has to be very fast because the text will be validated in real-time.
  // TODO: consider returning early if the text has whitespaces.
  fun validate(potentialUrl: String): Result {
    TODO()
  }
}
