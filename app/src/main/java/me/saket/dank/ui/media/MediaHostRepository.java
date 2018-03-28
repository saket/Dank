package me.saket.dank.ui.media;

import static java.util.Collections.unmodifiableList;

import android.support.annotation.CheckResult;

import com.nytimes.android.external.fs3.filesystem.FileSystem;
import com.nytimes.android.external.store3.base.impl.MemoryPolicy;
import com.nytimes.android.external.store3.base.impl.Store;
import com.nytimes.android.external.store3.base.impl.StoreBuilder;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.Moshi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Lazy;
import io.reactivex.Observable;
import io.reactivex.Single;
import me.saket.dank.BuildConfig;
import me.saket.dank.cache.DiskLruCachePathResolver;
import me.saket.dank.cache.StoreFilePersister;
import me.saket.dank.data.CachedResolvedLinkInfo;
import me.saket.dank.data.FileUploadProgressEvent;
import me.saket.dank.data.exceptions.ImgurApiRequestRateLimitReachedException;
import me.saket.dank.data.exceptions.ImgurApiUploadRateLimitReachedException;
import me.saket.dank.ui.giphy.GiphyGif;
import me.saket.dank.ui.giphy.GiphyRepository;
import me.saket.dank.ui.media.gfycat.GfycatRepository;
import me.saket.dank.urlparser.GfycatLink;
import me.saket.dank.urlparser.GfycatUnresolvedLink;
import me.saket.dank.urlparser.ImgurAlbumLink;
import me.saket.dank.urlparser.ImgurAlbumUnresolvedLink;
import me.saket.dank.urlparser.ImgurLink;
import me.saket.dank.urlparser.MediaLink;
import me.saket.dank.urlparser.StreamableLink;
import me.saket.dank.urlparser.StreamableUnresolvedLink;
import me.saket.dank.urlparser.UnresolvedMediaLink;
import me.saket.dank.urlparser.UrlParser;
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
  private final Lazy<GfycatRepository> gfycatRepository;

  @Inject
  public MediaHostRepository(
      StreamableRepository streamableRepository,
      ImgurRepository imgurRepository,
      FileSystem cacheFileSystem,
      Moshi moshi,
      GiphyRepository giphyRepository,
      Lazy<UrlParser> urlParser,
      Lazy<GfycatRepository> gfycatRepository)
  {
    this.streamableRepository = streamableRepository;
    this.imgurRepository = imgurRepository;
    this.giphyRepository = giphyRepository;
    this.urlParser = urlParser;
    this.gfycatRepository = gfycatRepository;

    StoreFilePersister.JsonParser<MediaLink> jsonParser = new MediaLinkStoreJsonParser(moshi);
    DiskLruCachePathResolver<MediaLink> pathResolver = new DiskLruCachePathResolver<MediaLink>() {
      @Override
      protected String resolveIn64Letters(MediaLink key) {
        return key.getClass().getSimpleName() + "_" + Urls.parseFileNameWithExtension(key.unparsedUrl());
      }
    };

    cacheStore = StoreBuilder.<MediaLink, MediaLink>key()
        .fetcher(unresolvedMediaLink -> resolveFromRemote(unresolvedMediaLink))
        .memoryPolicy(MemoryPolicy.builder()
            .setMemorySize(100)
            .setExpireAfterWrite(24)
            .setExpireAfterTimeUnit(TimeUnit.HOURS)
            .build())
        .persister(new StoreFilePersister<>(cacheFileSystem, pathResolver, jsonParser))
        .open();
  }

  static class MediaLinkStoreJsonParser implements StoreFilePersister.JsonParser<MediaLink> {
    private Moshi moshi;

    public MediaLinkStoreJsonParser(Moshi moshi) {
      this.moshi = moshi;
    }

    @Override
    public MediaLink fromJson(BufferedSource jsonBufferedSource) throws IOException {
      CachedResolvedLinkInfo cachedResolvedLinkInfo = moshi.adapter(CachedResolvedLinkInfo.class).fromJson(jsonBufferedSource);
      //noinspection ConstantConditions
      if (cachedResolvedLinkInfo.streamableLink() != null) {
        return cachedResolvedLinkInfo.streamableLink();

      } else if (cachedResolvedLinkInfo.imgurAlbumLink() != null) {
        return cachedResolvedLinkInfo.imgurAlbumLink();

      } else if (cachedResolvedLinkInfo.imgurLink() != null) {
        return cachedResolvedLinkInfo.imgurLink();

      } else if (cachedResolvedLinkInfo.gfycatLink() != null) {
        return cachedResolvedLinkInfo.gfycatLink();

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
      } else if (value instanceof GfycatLink) {
        cachedResolvedLinkInfo = CachedResolvedLinkInfo.create(((GfycatLink) value));
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
      return streamableRepository
          .video(((StreamableUnresolvedLink) unresolvedLink).videoId())
          .cast(MediaLink.class);

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

    } else if (unresolvedLink instanceof GfycatUnresolvedLink) {
      return gfycatRepository.get()
          .gif(((GfycatUnresolvedLink) unresolvedLink).threeWordId())
          .cast(MediaLink.class);

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
