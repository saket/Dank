package me.saket.dank.ui.media;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

import me.saket.dank.data.MediaLink;

@AutoValue
public abstract class MediaAlbumItem implements Parcelable {
  public abstract MediaLink mediaLink();

  public static MediaAlbumItem create(MediaLink mediaLink) {
    return new AutoValue_MediaAlbumItem(mediaLink);
  }
}
