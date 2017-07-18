package me.saket.dank.utils;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

/**
 * Base class for a RecyclerView adapter that is backed by an list of objects. The name of this class uses "array" to
 * keep it consistent with {@link ArrayAdapter} (which doesn't work for a RecyclerView).
 *
 * @param <T>  Class type of objects in this adapter's data-set.
 * @param <VH> Class type of ViewHolder.
 */
public abstract class RecyclerViewArrayAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

  private @Nullable List<T> items;

  /**
   * @param items Initial items to show.
   */
  public RecyclerViewArrayAdapter(@Nullable List<T> items) {
    this.items = items;
  }

  public RecyclerViewArrayAdapter() {
    this(null);
  }

  @Override
  public final VH onCreateViewHolder(ViewGroup parent, int viewType) {
    return onCreateViewHolder(LayoutInflater.from(parent.getContext()), parent, viewType);
  }

  protected abstract VH onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType);

  public T getItem(int position) {
    return items != null ? items.get(position) : null;
  }

  @Override
  public int getItemCount() {
    return items != null ? items.size() : 0;
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  /**
   * Updates this adapter's data set and refreshes the RecyclerView.
   */
  public void updateDataAndNotifyDatasetChanged(List<T> items) {
    this.items = items;
    notifyDataSetChanged();
  }

  /**
   * Updates this adapter's data set and refreshes the RecyclerView.
   */
  public void updateData(List<T> items) {
    this.items = items;
  }

  public List<T> getData() {
    return items;
  }
}
