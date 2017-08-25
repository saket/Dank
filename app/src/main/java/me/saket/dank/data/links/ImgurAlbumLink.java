package me.saket.dank.data.links;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

@AutoValue
public abstract class ImgurAlbumLink extends MediaLink implements Parcelable {

  @Override
  public abstract String unparsedUrl();

  public abstract String albumUrl();

  public abstract String albumTitle();

  public abstract String coverImageUrl();

  public abstract List<ImgurLink> images();

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

  public ImgurAlbumLink withCoverImageUrl(String newCoverImageUrl) {
    return create(albumUrl(), albumTitle(), newCoverImageUrl, images());
  }

  public static ImgurAlbumLink create(String albumUrl, String albumTitle, String coverImageUrl, List<ImgurLink> images) {
    return new AutoValue_ImgurAlbumLink(albumUrl /* unparsedUrl */, albumUrl, albumTitle, coverImageUrl, images);
  }

  public static JsonAdapter<ImgurAlbumLink> jsonAdapter(Moshi moshi) {
    return new AutoValue_ImgurAlbumLink.MoshiJsonAdapter(moshi);
  }
}
