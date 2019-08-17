package me.saket.dank.urlparser;

import android.os.Parcelable;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

@AutoValue
public abstract class ImgurAlbumLink extends MediaLink implements Parcelable {

  @Override
  public abstract String unparsedUrl();

  public abstract String albumId();

  public abstract String albumUrl();

  @Nullable
  public abstract String albumTitle();

  public boolean hasAlbumTitle() {
    //noinspection ConstantConditions
    return albumTitle() != null && !albumTitle().isEmpty();
  }

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

  @Override
  public String cacheKey() {
    return cacheKeyWithClassName(albumId());
  }

  public ImgurAlbumLink withCoverImageUrl(String newCoverImageUrl) {
    return create(albumId(), albumUrl(), albumTitle(), newCoverImageUrl, images());
  }

  public static ImgurAlbumLink create(String albumId, String albumUrl, @Nullable String albumTitle, String coverImageUrl, List<ImgurLink> images) {
    return new AutoValue_ImgurAlbumLink(albumId, albumUrl /* unparsedUrl */, albumUrl, albumTitle, coverImageUrl, images);
  }

  public static JsonAdapter<ImgurAlbumLink> jsonAdapter(Moshi moshi) {
    return new AutoValue_ImgurAlbumLink.MoshiJsonAdapter(moshi);
  }
}
