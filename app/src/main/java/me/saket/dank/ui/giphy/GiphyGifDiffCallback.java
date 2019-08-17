package me.saket.dank.ui.giphy;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import me.saket.dank.utils.SimpleDiffUtilsCallbacks;

public class GiphyGifDiffCallback extends SimpleDiffUtilsCallbacks<GiphyGif> {

  public GiphyGifDiffCallback(List<GiphyGif> oldItems, List<GiphyGif> newItems) {
    super(oldItems, newItems);
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
  @Override
  public boolean areItemsTheSame(GiphyGif oldItem, GiphyGif newItem) {
    return oldItem.equals(newItem);
  }

  /**
   * Called by the DiffUtil when it wants to check whether two items have the same data.
   * DiffUtil uses this information to detect if the contents of an item has changed.
   * <p>
   * DiffUtil uses this method to check equality instead of {@link Object#equals(Object)}
   * so that you can change its behavior depending on your UI.
   * For example, if you are using DiffUtil with a
   * {@link RecyclerView.Adapter RecyclerView.Adapter}, you should
   * return whether the items' visual representations are the same.
   * <p>
   * This method is called only if {@link #areItemsTheSame(int, int)} returns
   * {@code true} for these items.
   *
   * @param oldItem The item in the old list
   * @param newItem The item in the new list which replaces the oldItem
   * @return True if the contents of the items are the same or false if they are different.
   */
  @Override
  protected boolean areContentsTheSame(GiphyGif oldItem, GiphyGif newItem) {
    return oldItem.id().equals(newItem.id());
  }
}
