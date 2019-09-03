package me.saket.dank.utils;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.jakewharton.rxbinding2.support.v7.widget.RxRecyclerViewAdapter;
import com.jakewharton.rxrelay2.PublishRelay;

import java.util.List;

/**
 * <p>
 * Base class for a RecyclerView adapter that is backed by an list of objects. The name of this class uses "array" to
 * keep it consistent with {@link ArrayAdapter} (which doesn't work for a RecyclerView).
 *
 * @param <T>  Class type of objects in this adapter's data-set.
 * @param <VH> Class type of ViewHolder.
 */
public abstract class RecyclerViewArrayAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

  private @Nullable List<T> items;
  private PublishRelay<List<T>> dataChanges;

  /**
   * @param items Initial items to show.
   */
  public RecyclerViewArrayAdapter(@Nullable List<T> items) {
    this.items = items;
  }

  public RecyclerViewArrayAdapter() {
    this(null);
  }

  protected abstract VH onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType);

  @Override
  public final VH onCreateViewHolder(ViewGroup parent, int viewType) {
    if (!hasStableIds()) {
      throw new AssertionError("Ugh.");
    }
    return onCreateViewHolder(LayoutInflater.from(parent.getContext()), parent, viewType);
  }

  public T getItem(int position) {
    if (items == null) {
      throw new NullPointerException();
    }
    return items.get(position);
  }

  @Override
  public int getItemCount() {
    return items != null ? items.size() : 0;
  }

  @Override
  public abstract long getItemId(int position);

  /**
   * Updates this adapter's data set and refreshes the RecyclerView.
   */
  public void updateDataAndNotifyDatasetChanged(@Nullable List<T> items) {
    this.items = items;
    notifyDataSetChanged();
    notifyChangesToDataStream();
  }

  /**
   * Updates this adapter's data set and refreshes the RecyclerView.
   */
  public void updateData(List<T> items) {
    this.items = items;
    notifyChangesToDataStream();
  }

  public List<T> getData() {
    return items;
  }

  private void notifyChangesToDataStream() {
    if (dataChanges != null) {
      if (items == null) {
        throw new AssertionError("Items are null.");
      }
      dataChanges.accept(items);
    }
  }

  /**
   * Because {@link RxRecyclerViewAdapter#dataChanges(RecyclerView.Adapter)} only emits when
   * {@link #notifyDataSetChanged()} is called and not for {@link #notifyItemInserted(int)}, etc.
   */
  @CheckResult
  public PublishRelay<List<T>> dataChanges() {
    if (dataChanges == null) {
      dataChanges = PublishRelay.create();
    }
    return dataChanges;
  }
}
