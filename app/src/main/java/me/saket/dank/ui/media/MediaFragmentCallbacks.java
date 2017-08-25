package me.saket.dank.ui.media;

import android.support.annotation.Nullable;

import net.dean.jraw.models.Thumbnails;

public interface MediaFragmentCallbacks {
  void onClickMediaItem();

  int getDeviceDisplayWidth();

  @Nullable
  Thumbnails getRedditSuppliedImages();
}
