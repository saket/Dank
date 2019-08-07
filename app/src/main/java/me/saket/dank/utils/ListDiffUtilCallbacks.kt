package me.saket.dank.utils

import android.support.v7.util.DiffUtil

@Suppress("DEPRECATION")
class ListDiffUtilCallbacks<T>(
  oldItems: List<T>,
  newItems: List<T>,
  private val itemCallback: DiffUtil.ItemCallback<T>
) :
  SimpleDiffUtilsCallbacks<T>(oldItems, newItems) {

  override fun areItemsTheSame(oldItem: T, newItem: T) = itemCallback.areItemsTheSame(oldItem, newItem)

  override fun areContentsTheSame(oldItem: T, newItem: T) = itemCallback.areContentsTheSame(oldItem, newItem)

  override fun getChangePayload(oldItem: T, newItem: T): Any? = itemCallback.getChangePayload(oldItem, newItem)
}
