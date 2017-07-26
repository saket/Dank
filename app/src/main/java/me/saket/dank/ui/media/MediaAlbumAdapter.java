package me.saket.dank.ui.media;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.utils.RecyclerViewArrayAdapter;
import me.saket.dank.widgets.ZoomableImageView;

public class MediaAlbumAdapter extends RecyclerViewArrayAdapter<MediaAlbumItem, MediaAlbumAdapter.AlbumImageViewHolder> {

  private final int deviceDisplayWidth;

  public MediaAlbumAdapter(int deviceDisplayWidth) {
    this.deviceDisplayWidth = deviceDisplayWidth;
    setHasStableIds(true);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  protected AlbumImageViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    return AlbumImageViewHolder.create(inflater, parent);
  }

  @Override
  public void onBindViewHolder(AlbumImageViewHolder holder, int position) {
    holder.bind(getItem(position), deviceDisplayWidth);
  }

  public static class AlbumImageViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.item_mediaviewer_image) ZoomableImageView imageView;

    public static AlbumImageViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new AlbumImageViewHolder(inflater.inflate(R.layout.list_item_mediaviewer_image, parent, false));
    }

    public AlbumImageViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    public void bind(MediaAlbumItem albumItem, int deviceDisplayWidth) {
      Glide.with(imageView.getContext())
          .load(albumItem.mediaLink().optimizedImageUrl(deviceDisplayWidth))
          .crossFade()
          .into(imageView);
    }
  }
}
