package me.saket.dank.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.data.InfiniteScrollHeaderFooter;

@Deprecated
public class InfiniteScrollFooterViewHolder extends RecyclerView.ViewHolder {

  @BindView(R.id.infinitescroll_footer_progress) View progressView;
  @BindView(R.id.infinitescroll_footer_error) TextView errorTextView;

  public static InfiniteScrollFooterViewHolder create(LayoutInflater inflater, ViewGroup container) {
    View progressItemView = inflater.inflate(R.layout.list_item_subreddit_pagination_footer, container, false);
    return new InfiniteScrollFooterViewHolder(progressItemView);
  }

  public InfiniteScrollFooterViewHolder(View itemView) {
    super(itemView);
    ButterKnife.bind(this, itemView);
  }

  @Deprecated
  public void bind(InfiniteScrollHeaderFooter footer) {
    progressView.setVisibility(footer.type() == InfiniteScrollHeaderFooter.Type.PROGRESS ? View.VISIBLE : View.GONE);

    errorTextView.setVisibility(footer.type() == InfiniteScrollHeaderFooter.Type.ERROR ? View.VISIBLE : View.GONE);
    if (footer.type() == InfiniteScrollHeaderFooter.Type.ERROR) {
      errorTextView.setText(footer.titleRes());
    }

    itemView.setOnClickListener(footer.onClickListener());
    itemView.setClickable(footer.onClickListener() != null);
  }
}
