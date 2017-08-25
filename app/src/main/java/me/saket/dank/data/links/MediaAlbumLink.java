package me.saket.dank.data.links;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class MediaAlbumLink extends MediaLink implements Parcelable {

  @Override
  public abstract String unparsedUrl();

  public abstract String albumUrl();

  public abstract String albumTitle();

  public abstract String coverImageUrl();

  public abstract List<MediaLink> images();

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

  public MediaAlbumLink withCoverImageUrl(String newCoverImageUrl) {
    return create(albumUrl(), albumTitle(), newCoverImageUrl, images());
  }

  public static MediaAlbumLink create(String albumUrl, String albumTitle, String coverImageUrl, List<MediaLink> images) {
    return new AutoValue_MediaAlbumLink(albumUrl /* unparsedUrl */, albumUrl, albumTitle, coverImageUrl, images);
  }
}
