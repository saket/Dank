package me.saket.dank.utils;

import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.auto.value.AutoValue;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.functions.Consumer;
import me.saket.dank.R;

/**
 * Contains a header progress View for indicating fresh data load and a footer progress View
 * for indicating more data load. Both header and footer offer error states.
 *
 * @param <T> Type of items in the wrapped adapter.
 */
public class InfiniteScrollRecyclerAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    implements Consumer<List<T>>
{

  private static final int VIEW_TYPE_HEADER = 20;
  private static final int VIEW_TYPE_FOOTER = 21;

  private RecyclerViewArrayAdapter<T, VH> wrappedAdapter;
  private HeaderFooterInfo activeHeaderInfo = HeaderFooterInfo.createHidden();
  private HeaderFooterInfo activeFooterInfo = HeaderFooterInfo.createHidden();
  private RecyclerView recyclerView;

  @AutoValue
  public abstract static class HeaderFooterInfo {
    enum Type {
      PROGRESS,
      ERROR,
      HIDDEN,
      CUSTOM
    }

    public abstract Type type();

    @StringRes
    public abstract int titleRes();

    @DrawableRes
    public abstract int otherTypeIconRes();

    @ColorRes
    public abstract int otherTypeTextColor();

    @Nullable
    public abstract View.OnClickListener onClickListener();

    public static HeaderFooterInfo createHidden() {
      return new AutoValue_InfiniteScrollRecyclerAdapter_HeaderFooterInfo(Type.HIDDEN, 0, 0, 0, null);
    }

    public static HeaderFooterInfo createHeaderProgress(@StringRes int progressTitleRes) {
      return new AutoValue_InfiniteScrollRecyclerAdapter_HeaderFooterInfo(Type.PROGRESS, progressTitleRes, 0, 0, null);
    }

    public static HeaderFooterInfo createFooterProgress() {
      return new AutoValue_InfiniteScrollRecyclerAdapter_HeaderFooterInfo(Type.PROGRESS, 0, 0, 0, null);
    }

    public static HeaderFooterInfo createError(@StringRes int errorTitleRes, View.OnClickListener onRetryClickListener) {
      return new AutoValue_InfiniteScrollRecyclerAdapter_HeaderFooterInfo(Type.ERROR, errorTitleRes, 0, 0, onRetryClickListener);
    }

    public static HeaderFooterInfo createCustom(@StringRes int titleRes, View.OnClickListener onClickListener) {
      return new AutoValue_InfiniteScrollRecyclerAdapter_HeaderFooterInfo(Type.CUSTOM, titleRes, 0, 0, onClickListener);
    }
  }

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

  @Override
  public void accept(List<T> items) {
    wrappedAdapter.updateDataAndNotifyDatasetChanged(items);
  }

  public void setHeader(HeaderFooterInfo headerInfo) {
    if (activeHeaderInfo == headerInfo) {
      return;
    }
    recyclerView.post(() -> {
      activeHeaderInfo = headerInfo;
      notifyDataSetChanged();
    });
  }

  public void setFooter(HeaderFooterInfo footerInfo) {
    if (activeFooterInfo == footerInfo) {
      return;
    }
    recyclerView.post(() -> {
      activeFooterInfo = footerInfo;
      notifyDataSetChanged();
    });
  }

  public void setFooterWithoutNotifyingDataSetChanged(HeaderFooterInfo footerInfo) {
    if (activeFooterInfo == footerInfo) {
      return;
    }
    recyclerView.post(() -> activeFooterInfo = footerInfo);
  }

  public boolean isWrappedAdapterItem(int position) {
    return !(isHeaderItem(position) || isFooterItem(position));
  }

  public T getItemInWrappedAdapter(int position) {
    return wrappedAdapter.getItem(position - getVisibleHeaderItemCount());
  }

  @Override
  public void onAttachedToRecyclerView(RecyclerView recyclerView) {
    this.recyclerView = recyclerView;
    super.onAttachedToRecyclerView(recyclerView);
  }

  @Override
  public int getItemViewType(int position) {
    if (isHeaderItem(position)) {
      return VIEW_TYPE_HEADER;

    } else if (isFooterItem(position)) {
      return VIEW_TYPE_FOOTER;

    } else {
      int wrappedItemType = wrappedAdapter.getItemViewType(position - getVisibleHeaderItemCount());
      if (wrappedItemType == VIEW_TYPE_HEADER || wrappedItemType == VIEW_TYPE_FOOTER) {
        throw new IllegalStateException("Use another viewType value");
      }
      return wrappedItemType;
    }
  }

  @Override
  public long getItemId(int position) {
    if (isHeaderItem(position)) {
      return -VIEW_TYPE_HEADER;

    } else if (isFooterItem(position)) {
      return -VIEW_TYPE_FOOTER;

    } else {
      return wrappedAdapter.getItemId(position - getVisibleHeaderItemCount());
    }
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == VIEW_TYPE_HEADER) {
      return HeaderViewHolder.create(LayoutInflater.from(parent.getContext()), parent);

    } else if (viewType == VIEW_TYPE_FOOTER) {
      return FooterViewHolder.create(LayoutInflater.from(parent.getContext()), parent);

    } else {
      return wrappedAdapter.onCreateViewHolder(parent, viewType);
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    switch (getItemViewType(position)) {
      case VIEW_TYPE_HEADER:
        ((HeaderViewHolder) holder).bind(activeHeaderInfo);
        break;

      case VIEW_TYPE_FOOTER:
        ((FooterViewHolder) holder).bind(activeFooterInfo);
        break;

      default:
        //noinspection unchecked
        wrappedAdapter.onBindViewHolder((VH) holder, position - getVisibleHeaderItemCount());
    }
  }

  @Override
  public int getItemCount() {
    return wrappedAdapter.getItemCount() + getVisibleHeaderItemCount() + getVisibleFooterItemCount();
  }

  private boolean isHeaderItem(int position) {
    return isHeaderVisible() && position == 0;
  }

  private boolean isFooterItem(int position) {
    return isFooterVisible() && position == getItemCount() - 1;
  }

  private int getVisibleHeaderItemCount() {
    return isHeaderVisible() ? 1 : 0;
  }

  private int getVisibleFooterItemCount() {
    return isFooterVisible() ? 1 : 0;
  }

  private boolean isHeaderVisible() {
    return activeHeaderInfo.type() != HeaderFooterInfo.Type.HIDDEN;
  }

  private boolean isFooterVisible() {
    return activeFooterInfo.type() != HeaderFooterInfo.Type.HIDDEN;
  }

  static class HeaderViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.infinitescroll_header_progress_container) View progressContainer;
    @BindView(R.id.infinitescroll_footer_progress_title) TextView progressTextView;
    @BindView(R.id.infinitescroll_header_error) TextView errorTextView;
    @BindView(R.id.infinitescroll_header_custom_event) TextView customEventTextView;

    public static HeaderViewHolder create(LayoutInflater inflater, ViewGroup container) {
      View progressItemView = inflater.inflate(R.layout.list_item_infinitescroll_header, container, false);
      return new HeaderViewHolder(progressItemView);
    }

    public HeaderViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    public void bind(HeaderFooterInfo headerInfo) {
      progressContainer.setVisibility(headerInfo.type() == HeaderFooterInfo.Type.PROGRESS ? View.VISIBLE : View.GONE);
      if (headerInfo.type() == HeaderFooterInfo.Type.PROGRESS) {
        progressTextView.setText(headerInfo.titleRes());
      }

      errorTextView.setVisibility(headerInfo.type() == HeaderFooterInfo.Type.ERROR ? View.VISIBLE : View.GONE);
      if (headerInfo.type() == HeaderFooterInfo.Type.ERROR) {
        errorTextView.setText(headerInfo.titleRes());
      }

      customEventTextView.setVisibility(headerInfo.type() == HeaderFooterInfo.Type.CUSTOM ? View.VISIBLE : View.GONE);
      if (headerInfo.type() == HeaderFooterInfo.Type.CUSTOM) {
        customEventTextView.setText(headerInfo.titleRes());
      }

      itemView.setOnClickListener(headerInfo.onClickListener());
      itemView.setClickable(headerInfo.onClickListener() != null);
    }
  }

  static class FooterViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.infinitescroll_footer_progress) View progressView;
    @BindView(R.id.infinitescroll_footer_error) TextView errorTextView;

    public static FooterViewHolder create(LayoutInflater inflater, ViewGroup container) {
      View progressItemView = inflater.inflate(R.layout.list_item_infinitescroll_footer, container, false);
      return new FooterViewHolder(progressItemView);
    }

    public FooterViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    public void bind(HeaderFooterInfo footerInfo) {
      progressView.setVisibility(footerInfo.type() == HeaderFooterInfo.Type.PROGRESS ? View.VISIBLE : View.GONE);

      errorTextView.setVisibility(footerInfo.type() == HeaderFooterInfo.Type.ERROR ? View.VISIBLE : View.GONE);
      if (footerInfo.type() == HeaderFooterInfo.Type.ERROR) {
        errorTextView.setText(footerInfo.titleRes());
      }

      itemView.setOnClickListener(footerInfo.onClickListener());
      itemView.setClickable(footerInfo.onClickListener() != null);
    }
  }
}
