package me.saket.dank.utils;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

import io.reactivex.functions.Consumer;
import me.saket.dank.data.InfiniteScrollHeaderFooter;

/**
 * Contains a header progress View for indicating fresh data load and a footer progress View
 * for indicating more data load. Both header and footer offer error states.
 *
 * @param <T> Type of items in the wrapped adapter.
 */
public class InfiniteScrollRecyclerAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    implements Consumer<List<T>>
{

  private static final int VIEW_TYPE_FOOTER = 21;

  private RecyclerViewArrayAdapter<T, VH> wrappedAdapter;
  private InfiniteScrollHeaderFooter activeFooterInfo = InfiniteScrollHeaderFooter.createHidden();
  private RecyclerView recyclerView;

  public static <T, VH extends RecyclerView.ViewHolder> InfiniteScrollRecyclerAdapter<T, VH> wrap(RecyclerViewArrayAdapter<T, VH> adapterToWrap) {
    return new InfiniteScrollRecyclerAdapter<>(adapterToWrap);
  }

  private InfiniteScrollRecyclerAdapter(RecyclerViewArrayAdapter<T, VH> adapterToWrap) {
    this.wrappedAdapter = adapterToWrap;
    setHasStableIds(adapterToWrap.hasStableIds());

    wrappedAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
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
  public void accept(List<T> items) {
    wrappedAdapter.updateDataAndNotifyDatasetChanged(items);
  }

  public void setFooter(InfiniteScrollHeaderFooter footerInfo) {
    if (activeFooterInfo == footerInfo) {
      return;
    }
    recyclerView.post(() -> {
      activeFooterInfo = footerInfo;
      notifyDataSetChanged();
    });
  }

  public boolean isWrappedAdapterItem(int position) {
    return !isFooterItem(position);
  }

  public T getItemInWrappedAdapter(int position) {
    return wrappedAdapter.getItem(position);
  }

  @Override
  public void onAttachedToRecyclerView(RecyclerView recyclerView) {
    this.recyclerView = recyclerView;
    super.onAttachedToRecyclerView(recyclerView);
  }

  @Override
  public int getItemViewType(int position) {
    if (isFooterItem(position)) {
      return VIEW_TYPE_FOOTER;

    } else {
      int wrappedItemType = wrappedAdapter.getItemViewType(position);
      if (wrappedItemType == VIEW_TYPE_FOOTER) {
        throw new IllegalStateException("Use another viewType value");
      }
      return wrappedItemType;
    }
  }

  @Override
  public long getItemId(int position) {
    if (isFooterItem(position)) {
      return -VIEW_TYPE_FOOTER;

    } else {
      return wrappedAdapter.getItemId(position);
    }
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == VIEW_TYPE_FOOTER) {
      return InfiniteScrollFooterViewHolder.create(LayoutInflater.from(parent.getContext()), parent);

    } else {
      return wrappedAdapter.onCreateViewHolder(parent, viewType);
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    switch (getItemViewType(position)) {
      case VIEW_TYPE_FOOTER:
        ((InfiniteScrollFooterViewHolder) holder).bind(activeFooterInfo);
        break;

      default:
        //noinspection unchecked
        wrappedAdapter.onBindViewHolder((VH) holder, position);
    }
  }

  @Override
  public int getItemCount() {
    return wrappedAdapter.getItemCount() + getVisibleFooterItemCount();
  }

  private boolean isFooterItem(int position) {
    return isFooterVisible() && position == getItemCount() - 1;
  }

  private int getVisibleFooterItemCount() {
    return isFooterVisible() ? 1 : 0;
  }

  private boolean isFooterVisible() {
    return activeFooterInfo.type() != InfiniteScrollHeaderFooter.Type.HIDDEN;
  }
}
