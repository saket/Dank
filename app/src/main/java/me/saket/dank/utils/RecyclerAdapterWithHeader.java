package me.saket.dank.utils;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * An adapter that wraps in another adapter for adding a header row in the list.
 *
 * @param <HVH> Type of ViewHolder for the header.
 * @param <VH>  Type of ViewHolder for the child items.
 */
public abstract class RecyclerAdapterWithHeader<HVH extends RecyclerView.ViewHolder, VH extends RecyclerView.ViewHolder>
    extends RecyclerViewArrayAdapter<Object, VH>
{

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
        notifyItemRangeChanged(positionStart + getVisibleHeaderItemCount(), itemCount);
      }

      @Override
      public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
        notifyItemRangeChanged(positionStart + getVisibleHeaderItemCount(), itemCount, payload);
      }

      @Override
      public void onItemRangeInserted(int positionStart, int itemCount) {
        notifyItemRangeInserted(positionStart + getVisibleHeaderItemCount(), itemCount);
      }

      @Override
      public void onItemRangeRemoved(int positionStart, int itemCount) {
        notifyItemRangeRemoved(positionStart + getVisibleHeaderItemCount(), itemCount);
      }

      @Override
      public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
        // No notifyItemRangeMoved()? :/
        notifyItemRangeChanged(fromPosition + getVisibleHeaderItemCount(), toPosition + getVisibleHeaderItemCount() + itemCount);
      }
    });
  }

  protected abstract boolean isHeaderVisible();

  protected abstract HVH onCreateHeaderViewHolder(View headerView);

  protected abstract void onBindHeaderViewHolder(HVH holder, int position);

  protected abstract Object getHeaderItem();

  @Override
  public void updateData(List<Object> items) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getItemViewType(int position) {
    return isHeaderItem(position) ? VIEW_TYPE_HEADER : adapterToWrap.getItemViewType(position - getVisibleHeaderItemCount());
  }

  @Override
  protected VH onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    //noinspection unchecked
    return viewType == VIEW_TYPE_HEADER ? (VH) onCreateHeaderViewHolder(headerView) : adapterToWrap.onCreateViewHolder(parent, viewType);
  }

  @Override
  public long getItemId(int position) {
    return isHeaderItem(position) ? -VIEW_TYPE_HEADER : adapterToWrap.getItemId(position - getVisibleHeaderItemCount());
  }

  @Override
  public void onBindViewHolder(VH holder, int position) {
    if (getItemViewType(position) == VIEW_TYPE_HEADER) {
      //noinspection unchecked
      onBindHeaderViewHolder((HVH) holder, position);
    } else {
      //noinspection unchecked
      adapterToWrap.onBindViewHolder(holder, position - getVisibleHeaderItemCount());
    }
  }

  @Override
  public int getItemCount() {
    return getVisibleHeaderItemCount() + adapterToWrap.getItemCount();
  }

  public Object getItem(int position) {
    return position == 0 ? getHeaderItem() : adapterToWrap.getItem(position);
  }

  public int getVisibleHeaderItemCount() {
    return isHeaderVisible() ? 1 : 0;
  }

  public boolean isHeaderItem(int position) {
    return isHeaderVisible() && position == 0;
  }
}
