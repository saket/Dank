package me.saket.dank.ui.media;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import java.util.List;

public class MediaAlbumPagerAdapter extends FragmentStatePagerAdapter {

  private List<MediaAlbumItem> albumItems;
  private BaseMediaViewerFragment activeFragment;

  public MediaAlbumPagerAdapter(FragmentManager manager) {
    super(manager);
  }

  public void setAlbumItems(List<MediaAlbumItem> albumItems) {
    this.albumItems = albumItems;
    notifyDataSetChanged();
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

  @Override
  public void setPrimaryItem(ViewGroup container, int position, Object object) {
    super.setPrimaryItem(container, position, object);
    activeFragment = ((BaseMediaViewerFragment) object);
  }

  public BaseMediaViewerFragment getActiveFragment() {
    return activeFragment;
  }

  public List<MediaAlbumItem> getDataSet() {
    return albumItems;
  }
}
