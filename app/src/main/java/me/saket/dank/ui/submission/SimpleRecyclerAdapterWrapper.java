package me.saket.dank.ui.submission;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

/**
 * An adapter that delegates all calls to a wrapped adapter, except for create and bind.
 */
public class SimpleRecyclerAdapterWrapper<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

  private RecyclerView.Adapter<VH> adapterToWrap;

  public SimpleRecyclerAdapterWrapper(RecyclerView.Adapter<VH> adapterToWrap) {
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
  public VH onCreateViewHolder(ViewGroup parent, int viewType) {
    return null;
  }

  @Override
  public void onBindViewHolder(VH holder, int position) {

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
}
