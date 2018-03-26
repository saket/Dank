package me.saket.dank.ui.media;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

import me.saket.dank.urlparser.MediaLink;

@AutoValue
public abstract class MediaAlbumItem implements Parcelable {

  public abstract MediaLink mediaLink();

  public abstract boolean highDefinitionEnabled();

  public static MediaAlbumItem create(MediaLink mediaLink, boolean highDefinitionEnabled) {
    return new AutoValue_MediaAlbumItem(mediaLink, highDefinitionEnabled);
  }
}
