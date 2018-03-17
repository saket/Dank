package me.saket.dank.ui.media;

import static java.util.Collections.unmodifiableList;

import android.support.annotation.CheckResult;

import com.nytimes.android.external.fs3.filesystem.FileSystem;
import com.nytimes.android.external.store3.base.impl.Store;
import com.nytimes.android.external.store3.base.impl.StoreBuilder;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.Moshi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Lazy;
import io.reactivex.Observable;
import io.reactivex.Single;
import me.saket.dank.BuildConfig;
import me.saket.dank.data.CachedResolvedLinkInfo;
import me.saket.dank.data.FileUploadProgressEvent;
import me.saket.dank.data.ImgurImage;
import me.saket.dank.data.ImgurRepository;
import me.saket.dank.data.ImgurUploadResponse;
import me.saket.dank.data.exceptions.ImgurApiRequestRateLimitReachedException;
import me.saket.dank.data.exceptions.ImgurApiUploadRateLimitReachedException;
import me.saket.dank.data.links.ImgurAlbumLink;
import me.saket.dank.data.links.ImgurAlbumUnresolvedLink;
import me.saket.dank.data.links.ImgurLink;
import me.saket.dank.data.links.MediaLink;
import me.saket.dank.data.links.StreamableLink;
import me.saket.dank.data.links.StreamableUnresolvedLink;
import me.saket.dank.data.links.UnresolvedMediaLink;
import me.saket.dank.ui.giphy.GiphyGif;
import me.saket.dank.ui.giphy.GiphyRepository;
import me.saket.dank.urlparser.UrlParser;
import me.saket.dank.utils.DiskLruCachePathResolver;
import me.saket.dank.utils.StoreFilePersister;
import me.saket.dank.utils.StreamableRepository;
import me.saket.dank.utils.Urls;
import okio.BufferedSource;

/**
 * Entry point for accessing all media services.
 */
@Singleton
public class MediaHostRepository {

  private final StreamableRepository streamableRepository;
  private final ImgurRepository imgurRepository;
  private final GiphyRepository giphyRepository;
  private final Store<MediaLink, MediaLink> cacheStore;
  private final Lazy<UrlParser> urlParser;

  @Inject
  public MediaHostRepository(
      StreamableRepository streamableRepository,
      ImgurRepository imgurRepository,
      FileSystem cacheFileSystem,
      Moshi moshi,
      GiphyRepository giphyRepository,
      Lazy<UrlParser> urlParser)
  {
    this.streamableRepository = streamableRepository;
    this.imgurRepository = imgurRepository;
    this.giphyRepository = giphyRepository;
    this.urlParser = urlParser;

    StoreFilePersister.JsonParser<MediaLink> jsonParser = new MediaLinkStoreJsonParser(moshi);
    DiskLruCachePathResolver<MediaLink> pathResolver = new DiskLruCachePathResolver<MediaLink>() {
      @Override
      protected String resolveIn64Letters(MediaLink key) {
        return key.getClass().getSimpleName() + "_" + Urls.parseFileNameWithExtension(key.unparsedUrl());
      }
    };

    cacheStore = StoreBuilder.<MediaLink, MediaLink>key()
        .fetcher(unresolvedMediaLink -> resolveFromRemote(unresolvedMediaLink))
        .persister(new StoreFilePersister<>(cacheFileSystem, pathResolver, jsonParser))
        .open();
  }

  public static class MediaLinkStoreJsonParser implements StoreFilePersister.JsonParser<MediaLink> {
    private Moshi moshi;

    public MediaLinkStoreJsonParser(Moshi moshi) {
      this.moshi = moshi;
    }

    @Override
    public MediaLink fromJson(BufferedSource jsonBufferedSource) throws IOException {
      CachedResolvedLinkInfo cachedResolvedLinkInfo = moshi.adapter(CachedResolvedLinkInfo.class).fromJson(jsonBufferedSource);
      //noinspection ConstantConditions
      if (cachedResolvedLinkInfo.cachedStreamableLink() != null) {
        return cachedResolvedLinkInfo.cachedStreamableLink();

      } else if (cachedResolvedLinkInfo.cachedImgurAlbumLink() != null) {
        return cachedResolvedLinkInfo.cachedImgurAlbumLink();

      } else if (cachedResolvedLinkInfo.cachedImgurLink() != null) {
        return cachedResolvedLinkInfo.cachedImgurLink();

      } else {
        throw new JsonDataException("Unknown type: " + cachedResolvedLinkInfo);
      }
    }

    @Override
    public String toJson(MediaLink value) {
      CachedResolvedLinkInfo cachedResolvedLinkInfo;

      if (value instanceof StreamableLink) {
        cachedResolvedLinkInfo = CachedResolvedLinkInfo.create(((StreamableLink) value));
      } else if (value instanceof ImgurAlbumLink) {
        cachedResolvedLinkInfo = CachedResolvedLinkInfo.create(((ImgurAlbumLink) value));
      } else if (value instanceof ImgurLink) {
        cachedResolvedLinkInfo = CachedResolvedLinkInfo.create(((ImgurLink) value));
      } else {
        throw new JsonDataException("Unknown type: " + value);
      }

      return moshi.adapter(CachedResolvedLinkInfo.class).toJson(cachedResolvedLinkInfo);
    }
  }

  /**
   * Remember to handle {@link ImgurApiRequestRateLimitReachedException}.
   */
  public Single<MediaLink> resolveActualLinkIfNeeded(MediaLink unresolvedLink) {
    if (unresolvedLink instanceof UnresolvedMediaLink) {
      return cacheStore.get(unresolvedLink);
    } else {
      return Single.just(unresolvedLink);
    }
  }

  private Single<MediaLink> resolveFromRemote(MediaLink unresolvedLink) {
    if (unresolvedLink instanceof StreamableUnresolvedLink) {
      return streamableRepository.video(((StreamableUnresolvedLink) unresolvedLink).videoId()).cast(MediaLink.class);

    } else if (unresolvedLink instanceof ImgurAlbumUnresolvedLink) {
      return imgurRepository.gallery(((ImgurAlbumUnresolvedLink) unresolvedLink))
          .map(imgurResponse -> {
            if (imgurResponse.isAlbum()) {
              String albumUrl = ((ImgurAlbumUnresolvedLink) unresolvedLink).albumUrl();
              List<ImgurLink> images = unmodifiableList(convertImgurImagesToImgurMediaLinks(imgurResponse.images()));
              return ImgurAlbumLink.create(albumUrl, imgurResponse.albumTitle(), imgurResponse.albumCoverImageUrl(), images);

            } else {
              // Single image.
              return ((MediaLink) urlParser.get().parse(imgurResponse.images().get(0).url()));
            }
          });

    } else {
      return Single.just(unresolvedLink);
    }
  }

  private List<ImgurLink> convertImgurImagesToImgurMediaLinks(List<ImgurImage> imgurImages) {
    List<ImgurLink> imgurImageLinks = new ArrayList<>(imgurImages.size());
    for (ImgurImage imgurImage : imgurImages) {
      imgurImageLinks.add(urlParser.get().createImgurLink(imgurImage.url(), imgurImage.title(), imgurImage.description()));
    }
    return imgurImageLinks;
  }

  /**
   * Remember to handle {@link ImgurApiUploadRateLimitReachedException}.
   */
  @CheckResult
  public Observable<FileUploadProgressEvent<ImgurUploadResponse>> uploadImage(File image, String mimeType) {
    return imgurRepository.uploadImage(image, mimeType);
  }

  @CheckResult
  public Single<List<GiphyGif>> searchGifs(String searchQuery) {
    return giphyRepository.search(searchQuery);
  }

  public void clearCachedGifs() {
    if (!BuildConfig.DEBUG) {
      throw new AssertionError();
    }
    giphyRepository.clear();
  }

  public void clearCache() {
    if (!BuildConfig.DEBUG) {
      throw new AssertionError();
    }
    cacheStore.clear();
  }
}
