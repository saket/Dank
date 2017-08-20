package me.saket.dank.ui.media;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

public class MediaAlbumPagerAdapter extends FragmentStatePagerAdapter {

  private List<MediaAlbumItem> albumItems;

  public MediaAlbumPagerAdapter(FragmentManager manager, List<MediaAlbumItem> albumItems) {
    super(manager);
    this.albumItems = albumItems;
  }

  @Override
  public Fragment getItem(int position) {
    MediaAlbumItem mediaAlbumItem = albumItems.get(position);
    return mediaAlbumItem.mediaLink().isVideo()
        ? MediaVideoFragment.create(mediaAlbumItem)
        : MediaImageFragment.create(mediaAlbumItem);
  }

  @Override
  public int getCount() {
    return albumItems != null ? albumItems.size() : 0;
  }

  public List<MediaAlbumItem> getDataSet() {
    return albumItems;
  }
}
