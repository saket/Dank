package me.saket.dank.utils;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * An adapter that wraps in another adapter for adding a header row in the list.
 *
 * @param <VH> Type of ViewHolder for the header and the child items.
 */
public abstract class RecyclerAdapterWithHeader<VH extends RecyclerView.ViewHolder> extends RecyclerViewArrayAdapter<Object, VH> {

  private static final int VIEW_TYPE_HEADER = 99;

  private RecyclerViewArrayAdapter<?, VH> adapterToWrap;
  private View headerView;

  protected RecyclerAdapterWithHeader(RecyclerViewArrayAdapter<?, VH> adapterToWrap, View headerView) {
    this.adapterToWrap = adapterToWrap;
    this.headerView = headerView;
    setHasStableIds(adapterToWrap.hasStableIds());

    adapterToWrap.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
      @Override
      public void onChanged() {
        notifyDataSetChanged();
      }

      @Override
      public void onItemRangeChanged(int positionStart, int itemCount) {
        notifyItemRangeChanged(positionStart + 1, itemCount);
      }

      @Override
      public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
        notifyItemRangeChanged(positionStart + 1, itemCount, payload);
      }

      @Override
      public void onItemRangeInserted(int positionStart, int itemCount) {
        notifyItemRangeInserted(positionStart + 1, itemCount);
      }

      @Override
      public void onItemRangeRemoved(int positionStart, int itemCount) {
        notifyItemRangeRemoved(positionStart + 1, itemCount);
      }

      @Override
      public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
        // No notifyItemRangeMoved()? :/
        notifyItemRangeChanged(fromPosition + 1, toPosition + 1 + itemCount);
      }
    });
  }

  protected abstract VH onCreateHeaderViewHolder(View headerView);

  protected abstract void onBindHeaderViewHolder(VH holder, int position);

  protected abstract Object getHeaderItem();

  @Override
  public void updateData(List<Object> items) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected VH onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    return viewType == VIEW_TYPE_HEADER ? onCreateHeaderViewHolder(headerView) : adapterToWrap.onCreateViewHolder(parent, viewType);
  }

  @Override
  public long getItemId(int position) {
    return position == 0 ? -VIEW_TYPE_HEADER : adapterToWrap.getItemId(position - 1);
  }

  @Override
  public int getItemViewType(int position) {
    return position == 0 ? VIEW_TYPE_HEADER : adapterToWrap.getItemViewType(position - 1);
  }

  @Override
  public void onBindViewHolder(VH holder, int position) {
    if (getItemViewType(position) == VIEW_TYPE_HEADER) {
      onBindHeaderViewHolder(holder, position);
    } else {
      //noinspection unchecked
      adapterToWrap.onBindViewHolder(holder, position - 1);
    }
  }

  @Override
  public int getItemCount() {
    return 1 + adapterToWrap.getItemCount();
  }

  public Object getItem(int position) {
    return position == 0 ? getHeaderItem() : adapterToWrap.getItem(position);
  }
}
