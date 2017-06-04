package me.saket.dank.ui.submission;

import android.support.v7.widget.RecyclerView;

import java.util.List;

import me.saket.dank.utils.RecyclerViewArrayAdapter;

/**
 * An adapter that delegates all calls to a wrapped adapter, except for create and bind.
 */
public abstract class SimpleRecyclerAdapterWrapper<T, VH extends RecyclerView.ViewHolder> extends RecyclerViewArrayAdapter<T, VH> {

  private RecyclerViewArrayAdapter<T, VH> adapterToWrap;

  public SimpleRecyclerAdapterWrapper(RecyclerViewArrayAdapter<T, VH> adapterToWrap) {
    this.adapterToWrap = adapterToWrap;

    setHasStableIds(adapterToWrap.hasStableIds());

    adapterToWrap.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
      @Override
      public void onChanged() {
        notifyDataSetChanged();
      }

      @Override
      public void onItemRangeChanged(int positionStart, int itemCount) {
        notifyItemRangeChanged(positionStart, itemCount);
      }

      @Override
      public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
        notifyItemRangeChanged(positionStart, itemCount, payload);
      }

      @Override
      public void onItemRangeInserted(int positionStart, int itemCount) {
        notifyItemRangeInserted(positionStart, itemCount);
      }

      @Override
      public void onItemRangeRemoved(int positionStart, int itemCount) {
        notifyItemRangeRemoved(positionStart, itemCount);
      }

      @Override
      public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
        // No notifyItemRangeMoved()? :/
        notifyItemRangeChanged(fromPosition, toPosition + itemCount);
      }
    });
  }

  @Override
  public int getItemCount() {
    return adapterToWrap.getItemCount();
  }

  @Override
  public long getItemId(int position) {
    return adapterToWrap.getItemId(position);
  }

  @Override
  public int getItemViewType(int position) {
    return adapterToWrap.getItemViewType(position);
  }

  @Override
  public void updateData(List<T> items) {
    adapterToWrap.updateData(items);
  }
}
