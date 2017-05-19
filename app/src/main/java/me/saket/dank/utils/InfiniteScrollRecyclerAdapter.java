package me.saket.dank.utils;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashSet;
import java.util.Set;

import me.saket.dank.R;

/**
 * Contains a header progress View for indicating fresh data load and a footer progress View
 * for indicating more data load. Both header and footer offer error states.
 */
public class InfiniteScrollRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private static final int VIEW_TYPE_HEADER = 99;
  private static final int VIEW_TYPE_FOOTER = 100;

  private RecyclerView.Adapter adapterToWrap;
  private Set<ProgressType> visibleProgressTypes;

  public enum ProgressType {
    REFRESH_DATA_LOAD_PROGRESS,
    MORE_DATA_LOAD_PROGRESS
  }

  public static InfiniteScrollRecyclerAdapter wrap(RecyclerView.Adapter adapterToWrap) {
    return new InfiniteScrollRecyclerAdapter(adapterToWrap);
  }

  private InfiniteScrollRecyclerAdapter(RecyclerView.Adapter adapterToWrap) {
    this.adapterToWrap = adapterToWrap;
    this.visibleProgressTypes = new HashSet<>(3);
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

  public void setProgressVisible(ProgressType progressType, boolean visible) {
    if (visible == visibleProgressTypes.contains(progressType)) {
      return;
    }

    if (visible) {
      visibleProgressTypes.add(progressType);
    } else {
      visibleProgressTypes.remove(progressType);
    }

    // TODO: Use DiffUtils?
//    boolean isHeader = progressType == ProgressType.REFRESH_DATA_LOAD_PROGRESS;
//    if (visible) {
//      notifyItemInserted(isHeader ? 0 : getItemCount());
//
//    } else {
//      // Note: the footer item position should be itemCount-1, but it's resulting
//      // in the 2nd last item getting animated in a strange way. Using the itemCount
//      // also works. Not sure why.
//      notifyItemRemoved(isHeader ? 0 : getItemCount());
//    }
    notifyDataSetChanged();
  }

  @Override
  public long getItemId(int position) {
    if (isHeaderProgressItem(position)) {
      return VIEW_TYPE_HEADER;
    } else if (isFooterProgressItem(position)) {
      return VIEW_TYPE_FOOTER;
    } else {
      return adapterToWrap.getItemId(position - getVisibleHeaderItemCount());
    }
  }

  @Override
  public int getItemViewType(int position) {
    if (isHeaderProgressItem(position)) {
      return VIEW_TYPE_HEADER;
    } else if (isFooterProgressItem(position)) {
      return VIEW_TYPE_FOOTER;
    } else {
      return adapterToWrap.getItemViewType(position - getVisibleHeaderItemCount());
    }
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == VIEW_TYPE_HEADER) {
      return HeaderProgressViewHolder.create(LayoutInflater.from(parent.getContext()), parent);
    } else if (viewType == VIEW_TYPE_FOOTER) {
      return FooterProgressViewHolder.create(LayoutInflater.from(parent.getContext()), parent);
    } else {
      return adapterToWrap.onCreateViewHolder(parent, viewType);
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    int viewType = getItemViewType(position);
    if (viewType != VIEW_TYPE_HEADER && viewType != VIEW_TYPE_FOOTER) {
      //noinspection unchecked
      adapterToWrap.onBindViewHolder(holder, position - getVisibleHeaderItemCount());
    }
  }

  @Override
  public int getItemCount() {
    return adapterToWrap.getItemCount() + getVisibleHeaderItemCount() + getVisibleFooterItemCount();
  }

  private boolean isHeaderProgressItem(int position) {
    return isProgressVisible(ProgressType.REFRESH_DATA_LOAD_PROGRESS) && position == 0;
  }

  private boolean isFooterProgressItem(int position) {
    return isProgressVisible(ProgressType.MORE_DATA_LOAD_PROGRESS) && position == getItemCount() - 1;
  }

  private int getVisibleHeaderItemCount() {
    return isProgressVisible(ProgressType.REFRESH_DATA_LOAD_PROGRESS) ? 1 : 0;
  }

  private int getVisibleFooterItemCount() {
    return isProgressVisible(ProgressType.MORE_DATA_LOAD_PROGRESS) ? 1 : 0;
  }

  private boolean isProgressVisible(ProgressType type) {
    return visibleProgressTypes.contains(type);
  }

  // TODO: Layout.
  static class HeaderProgressViewHolder extends RecyclerView.ViewHolder {
    public static HeaderProgressViewHolder create(LayoutInflater inflater, ViewGroup container) {
      View progressItemView = inflater.inflate(R.layout.list_item_progress_indicator, container, false);
      return new HeaderProgressViewHolder(progressItemView);
    }

    public HeaderProgressViewHolder(View itemView) {
      super(itemView);
    }
  }

  static class FooterProgressViewHolder extends RecyclerView.ViewHolder {
    public static FooterProgressViewHolder create(LayoutInflater inflater, ViewGroup container) {
      View progressItemView = inflater.inflate(R.layout.list_item_progress_indicator, container, false);
      return new FooterProgressViewHolder(progressItemView);
    }

    public FooterProgressViewHolder(View itemView) {
      super(itemView);
    }
  }

}
