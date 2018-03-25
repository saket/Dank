package me.saket.dank.ui.giphy;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

/**
 * A GIF on giphy.com.
 */
@AutoValue
public abstract class GiphyGif implements Parcelable {

  public abstract String id();

  public abstract String title();

  public abstract String url();

  public abstract String previewUrl();

  public static GiphyGif create(String id, String title, String url, String previewUrl) {
    return new AutoValue_GiphyGif(id, title, url, previewUrl);
  }
}
