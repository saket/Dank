package me.saket.dank.utils;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import me.saket.dank.R;

/**
 * An adapter that shows a load progress indicator item at the bottom.
 */
public class RecyclerAdapterWithProgressFooter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_FOOTER = 100;

    private boolean isProgressVisible;
    private RecyclerView.Adapter adapterToWrap;

    public static RecyclerAdapterWithProgressFooter wrap(RecyclerView.Adapter adapterToWrap) {
        return new RecyclerAdapterWithProgressFooter(adapterToWrap);
    }

    private RecyclerAdapterWithProgressFooter(RecyclerView.Adapter adapterToWrap) {
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

    public void setProgressVisible(boolean visible) {
        if (isProgressVisible != visible) {
            isProgressVisible = visible;

            if (visible) {
                notifyItemInserted(getItemCount());
            } else {
                notifyItemRemoved(getItemCount() - 1);
            }
        }
    }

    @Override
    public long getItemId(int position) {
        if (isProgressVisible && position == getItemCount() - 1) {
            return 99;
        } else {
            return adapterToWrap.getItemId(position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isProgressVisible) {
            return position < getItemCount() - 1 ? adapterToWrap.getItemViewType(position) : VIEW_TYPE_FOOTER;
        } else {
            return adapterToWrap.getItemViewType(position);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_FOOTER) {
            return ProgressViewHolder.create(LayoutInflater.from(parent.getContext()), parent);
        } else {
            return adapterToWrap.onCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) != VIEW_TYPE_FOOTER) {
            //noinspection unchecked
            adapterToWrap.onBindViewHolder(holder, position);
        }
    }

    @Override
    public int getItemCount() {
        int wrappedCount = adapterToWrap.getItemCount();
        return wrappedCount + (isProgressVisible ? 1 : 0);
    }

    static class ProgressViewHolder extends RecyclerView.ViewHolder {
        public static ProgressViewHolder create(LayoutInflater inflater, ViewGroup container) {
            View progressItemView = inflater.inflate(R.layout.list_item_progress_indicator, container, false);
            return new ProgressViewHolder(progressItemView);
        }

        public ProgressViewHolder(View itemView) {
            super(itemView);
        }
    }

}
