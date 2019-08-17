package me.saket.dank.data;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.urlparser.GfycatLink;
import me.saket.dank.urlparser.ImgurAlbumLink;
import me.saket.dank.urlparser.ImgurLink;
import me.saket.dank.urlparser.MediaLink;
import me.saket.dank.urlparser.StreamableLink;

/**
 * Used by {@link MediaHostRepository} to figure out the wrapped class type for deserializing the actual resolved link.
 */
@AutoValue
public abstract class CachedResolvedLinkInfo {

  @Nullable
  public abstract StreamableLink streamableLink();

  @Nullable
  public abstract ImgurLink imgurLink();

  @Nullable
  public abstract ImgurAlbumLink imgurAlbumLink();

  @Nullable
  public abstract GfycatLink gfycatLink();

  public static CachedResolvedLinkInfo create(StreamableLink streamableLink) {
    return new AutoValue_CachedResolvedLinkInfo(streamableLink, null, null, null);
  }

  public static CachedResolvedLinkInfo create(ImgurLink imgurLink) {
    return new AutoValue_CachedResolvedLinkInfo(null, imgurLink, null, null);
  }

  public static CachedResolvedLinkInfo create(ImgurAlbumLink imgurAlbumLink) {
    return new AutoValue_CachedResolvedLinkInfo(null, null, imgurAlbumLink, null);
  }

  public static CachedResolvedLinkInfo create(GfycatLink gfycatLink) {
    return new AutoValue_CachedResolvedLinkInfo(null, null, null, gfycatLink);
  }

  public static CachedResolvedLinkInfo create(MediaLink ignored) {
    throw new IllegalStateException("Use a specific create() method instead");
  }

  public static JsonAdapter<CachedResolvedLinkInfo> jsonAdapter(Moshi moshi) {
    return new AutoValue_CachedResolvedLinkInfo.MoshiJsonAdapter(moshi);
  }
}
