package me.saket.dank.ui.submission;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * An adapter that wraps in another adapter for adding a header row in the list.
 */
public class RecyclerAdapterWithHeader extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;

    private RecyclerView.Adapter adapterToWrap;
    private View headerView;

    public static RecyclerAdapterWithHeader wrap(RecyclerView.Adapter adapterToWrap, View headerView) {
        if (headerView.getParent() != null) {
            ((ViewGroup) headerView.getParent()).removeView(headerView);
        }

        return new RecyclerAdapterWithHeader(adapterToWrap, headerView);
    }

    private RecyclerAdapterWithHeader(RecyclerView.Adapter adapterToWrap, View headerView) {
        this.adapterToWrap = adapterToWrap;
        this.headerView = headerView;

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

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            return new HeaderViewHolder(headerView);
        } else {
            return adapterToWrap.onCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_HEADER : adapterToWrap.getItemViewType(position - 1);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) != VIEW_TYPE_HEADER) {
            //noinspection unchecked
            adapterToWrap.onBindViewHolder(holder, position - 1);
        }
    }

    @Override
    public int getItemCount() {
        return 1 + adapterToWrap.getItemCount();
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }

}
