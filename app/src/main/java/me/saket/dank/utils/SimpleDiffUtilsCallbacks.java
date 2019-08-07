package me.saket.dank.utils;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;

import java.util.List;

/**
 * DIffUtils.Callback + generics.
 */
@Deprecated
public abstract class SimpleDiffUtilsCallbacks<T> extends DiffUtil.Callback {

  private final List<T> oldItems;
  private final List<T> newItems;

  public SimpleDiffUtilsCallbacks(List<T> oldItems, List<T> newItems) {
    this.oldItems = oldItems;
    this.newItems = newItems;
  }

  /**
   * Called by the DiffUtil to decide whether two object represent the same Item.
   * <p>
   * For example, if your items have unique ids, this method should check their id equality.
   *
   * @param oldItem The item in the old list
   * @param newItem The item in the new list
   * @return True if the two items represent the same object or false if they are different.
   */
  public abstract boolean areItemsTheSame(T oldItem, T newItem);

  /**
   * Called by the DiffUtil when it wants to check whether two items have the same data.
   * DiffUtil uses this information to detect if the contents of an item has changed.
   * <p>
   * DiffUtil uses this method to check equality instead of {@link Object#equals(Object)}
   * so that you can change its behavior depending on your UI.
   * For example, if you are using DiffUtil with a
   * {@link android.support.v7.widget.RecyclerView.Adapter RecyclerView.Adapter}, you should
   * return whether the items' visual representations are the same.
   * <p>
   * This method is called only if {@link #areItemsTheSame(int, int)} returns
   * {@code true} for these items.
   *
   * @param oldItem The item in the old list
   * @param newItem The item in the new list which replaces the oldItem
   * @return True if the contents of the items are the same or false if they are different.
   */
  protected abstract boolean areContentsTheSame(T oldItem, T newItem);

  /**
   * When {@link #areItemsTheSame(int, int)} returns {@code true} for two items and
   * {@link #areContentsTheSame(int, int)} returns false for them, DiffUtil
   * calls this method to get a payload about the change.
   * <p>
   * For example, if you are using DiffUtil with {@link RecyclerView}, you can return the
   * particular field that changed in the item and your
   * {@link android.support.v7.widget.RecyclerView.ItemAnimator ItemAnimator} can use that
   * information to run the correct animation.
   * <p>
   * Default implementation returns {@code null}.
   *
   * @param oldItem The item in the old list
   * @param newItem The item in the new list
   * @return A payload object that represents the change between the two items.
   */
  @Nullable
  @SuppressWarnings("unused")
  public Object getChangePayload(T oldItem, T newItem) {
    return null;
  }

  @Override
  public final int getOldListSize() {
    return oldItems.size();
  }

  @Override
  public final int getNewListSize() {
    return newItems.size();
  }

  @Override
  public final boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
    T oldItem = oldItems.get(oldItemPosition);
    T newItem = newItems.get(newItemPosition);
    return areItemsTheSame(oldItem, newItem);
  }

  @Override
  public final boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
    T oldItem = oldItems.get(oldItemPosition);
    T newItem = newItems.get(newItemPosition);
    return areContentsTheSame(oldItem, newItem);
  }

  @Nullable
  @Override
  public final Object getChangePayload(int oldItemPosition, int newItemPosition) {
    T oldItem = oldItems.get(oldItemPosition);
    T newItem = newItems.get(newItemPosition);
    return getChangePayload(oldItem, newItem);
  }
}
