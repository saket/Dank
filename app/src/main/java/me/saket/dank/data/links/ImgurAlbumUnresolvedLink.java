package me.saket.dank.data.links;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

/**
 * A Imgur.com/gallery link, whose actual image count is unknown. It could be a single image/gif or an album.
 */
@AutoValue
public abstract class ImgurAlbumUnresolvedLink extends MediaLink implements Parcelable, UnresolvedMediaLink {

  public abstract String albumUrl();

  public abstract String albumId();

  @Override
  public Link.Type type() {
    return Link.Type.MEDIA_ALBUM;
  }

  @Override
  public String highQualityUrl() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String lowQualityUrl() {
    throw new UnsupportedOperationException();
  }

  public static ImgurAlbumUnresolvedLink create(String unresolvedUrl, String albumId) {
    return new AutoValue_ImgurAlbumUnresolvedLink(unresolvedUrl, unresolvedUrl, albumId);
  }
}
