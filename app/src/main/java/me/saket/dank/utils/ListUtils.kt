package me.saket.dank.utils

import java.util.*

/**
 * Moves the element from the specified source position to the specified
 * target position. (If the specified positions are equal, invoking this
 * method leaves the list unchanged.)
 *
 * @param fromPosition the index of the element to be moved.
 * @param toPosition the destination index for the element to be moved to.
 */
fun <T> MutableList<T>.move(fromPosition: Int, toPosition: Int) {
  if (fromPosition < toPosition) {
    for (i in fromPosition until toPosition) {
      Collections.swap(this, i, i + 1)
    }
  } else {
    for (i in fromPosition downTo toPosition + 1) {
      Collections.swap(this, i, i - 1)
    }
  }
}
