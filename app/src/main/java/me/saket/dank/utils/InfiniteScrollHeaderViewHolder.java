package me.saket.dank.utils;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.data.InfiniteScrollHeader;
import me.saket.dank.data.InfiniteScrollHeaderFooter;

public class InfiniteScrollHeaderViewHolder extends RecyclerView.ViewHolder {

  @BindView(R.id.infinitescroll_header_progress_container) View progressContainer;
  @BindView(R.id.infinitescroll_footer_progress_title) TextView progressTextView;
  @BindView(R.id.infinitescroll_header_error) TextView errorTextView;
  @BindView(R.id.infinitescroll_header_custom_event) TextView customEventTextView;

  public static InfiniteScrollHeaderViewHolder create(LayoutInflater inflater, ViewGroup container) {
    View progressItemView = inflater.inflate(R.layout.list_item_infinitescroll_header, container, false);
    return new InfiniteScrollHeaderViewHolder(progressItemView);
  }

  public InfiniteScrollHeaderViewHolder(View itemView) {
    super(itemView);
    ButterKnife.bind(this, itemView);
  }

  public void bind (InfiniteScrollHeader header) {
    progressContainer.setVisibility(header.type() == InfiniteScrollHeader.Type.PROGRESS ? View.VISIBLE : View.GONE);
    if (header.type() == InfiniteScrollHeader.Type.PROGRESS) {
      progressTextView.setText(header.titleRes());
    }

    errorTextView.setVisibility(header.type() == InfiniteScrollHeader.Type.ERROR ? View.VISIBLE : View.GONE);
    if (header.type() == InfiniteScrollHeader.Type.ERROR) {
      errorTextView.setText(header.titleRes());
    }

    customEventTextView.setVisibility(header.type() == InfiniteScrollHeader.Type.CUSTOM ? View.VISIBLE : View.GONE);
    if (header.type() == InfiniteScrollHeader.Type.CUSTOM) {
      customEventTextView.setText(header.titleRes());
    }
  }

  @Deprecated
  public void bind(InfiniteScrollHeaderFooter header) {
    progressContainer.setVisibility(header.type() == InfiniteScrollHeaderFooter.Type.PROGRESS ? View.VISIBLE : View.GONE);
    if (header.type() == InfiniteScrollHeaderFooter.Type.PROGRESS) {
      progressTextView.setText(header.titleRes());
    }

    errorTextView.setVisibility(header.type() == InfiniteScrollHeaderFooter.Type.ERROR ? View.VISIBLE : View.GONE);
    if (header.type() == InfiniteScrollHeaderFooter.Type.ERROR) {
      errorTextView.setText(header.titleRes());
    }

    customEventTextView.setVisibility(header.type() == InfiniteScrollHeaderFooter.Type.CUSTOM ? View.VISIBLE : View.GONE);
    if (header.type() == InfiniteScrollHeaderFooter.Type.CUSTOM) {
      customEventTextView.setText(header.titleRes());
    }

    itemView.setOnClickListener(header.onClickListener());
    itemView.setClickable(header.onClickListener() != null);
  }
}
