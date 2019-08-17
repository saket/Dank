package me.saket.dank.ui.giphy;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.CheckResult;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import me.saket.dank.R;
import me.saket.dank.utils.InfinitelyScrollableRecyclerViewAdapter;
import me.saket.dank.utils.RecyclerViewArrayAdapter;

public class GiphyAdapter extends RecyclerViewArrayAdapter<GiphyGif, GiphyAdapter.GiphyGifViewHolder>
    implements InfinitelyScrollableRecyclerViewAdapter, Consumer<List<GiphyGif>>
{

  private final ColorDrawable[] giphyPlaceholders;
  private Relay<GiphyGif> clickStream = PublishRelay.create();

  public GiphyAdapter(int[] giphyPlaceholderColors) {
    setHasStableIds(true);

    giphyPlaceholders = new ColorDrawable[giphyPlaceholderColors.length];
    for (int i = 0; i < giphyPlaceholderColors.length; i++) {
      giphyPlaceholders[i] = new ColorDrawable(giphyPlaceholderColors[i]);
    }
  }

  @CheckResult
  public Observable<GiphyGif> streamClicks() {
    return clickStream;
  }

  @Override
  protected GiphyGifViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    GiphyGifViewHolder holder = GiphyGifViewHolder.create(inflater, parent);
    holder.itemView.setOnClickListener(v -> clickStream.accept(getItem(holder.getAdapterPosition())));
    return holder;
  }

  @Override
  public void onBindViewHolder(GiphyGifViewHolder holder, int position) {
    GiphyGif giphyGif = getItem(position);
    Drawable randomPlaceholder = giphyPlaceholders[Math.abs(giphyGif.id().hashCode()) % giphyPlaceholders.length];
    holder.bind(giphyGif, randomPlaceholder);
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).id().hashCode();
  }

  /**
   * Decorators == headers, footers, etc.
   */
  @Override
  public int getItemCountMinusDecorators() {
    return getItemCount();
  }

  /**
   * Consume the given value.
   *
   * @param giphyGifs the value
   * @throws Exception on error
   */
  @Override
  public void accept(List<GiphyGif> giphyGifs) throws Exception {
    updateDataAndNotifyDatasetChanged(giphyGifs);
  }

  public static class GiphyGifViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.item_giphy_gif) ImageView imageView;
    @BindView(R.id.item_giphy_position) TextView positionView;

    public static GiphyGifViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new GiphyGifViewHolder(inflater.inflate(R.layout.list_item_giphy_search_result, parent, false));
    }

    public GiphyGifViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    public void bind(GiphyGif giphyGif, Drawable randomPlaceholder) {
      positionView.setText(String.valueOf(getAdapterPosition()));
      imageView.setContentDescription(giphyGif.title());

      Glide.with(imageView)
          .load(giphyGif.previewUrl())
          .apply(RequestOptions.placeholderOf(randomPlaceholder))
          .transition(DrawableTransitionOptions.withCrossFade())
          .into(imageView);
    }
  }
}
