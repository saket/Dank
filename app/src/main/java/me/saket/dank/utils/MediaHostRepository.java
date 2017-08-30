package me.saket.dank.utils;

import static java.util.Collections.unmodifiableList;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.nytimes.android.external.fs3.PathResolver;
import com.nytimes.android.external.fs3.filesystem.FileSystem;
import com.nytimes.android.external.store3.base.impl.Store;
import com.nytimes.android.external.store3.base.impl.StoreBuilder;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import net.dean.jraw.models.Thumbnails;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import me.saket.dank.data.ImgurImage;
import me.saket.dank.data.exceptions.ImgurApiRateLimitReachedException;
import me.saket.dank.data.links.ImgurAlbumLink;
import me.saket.dank.data.links.ImgurAlbumUnresolvedLink;
import me.saket.dank.data.links.ImgurLink;
import me.saket.dank.data.links.MediaLink;
import me.saket.dank.data.links.StreamableLink;
import me.saket.dank.data.links.StreamableUnresolvedLink;
import me.saket.dank.data.links.UnresolvedMediaLink;
import okio.BufferedSource;

@Singleton
public class MediaHostRepository {

  private final StreamableRepository streamableRepository;
  private final ImgurRepository imgurRepository;
  private final Store<MediaLink, MediaLink> cacheStore;

  @Inject
  public MediaHostRepository(StreamableRepository streamableRepository, ImgurRepository imgurRepository, FileSystem cacheFileSystem, Moshi moshi) {
    this.streamableRepository = streamableRepository;
    this.imgurRepository = imgurRepository;

    StoreFilePersister.JsonParser<MediaLink, MediaLink> jsonParser = createJsonParserForMediaLinkPersister(moshi);
    PathResolver<MediaLink> pathResolver = key -> key.getClass().getSimpleName() + "_" + Urls.parseFileNameWithExtension(key.unparsedUrl());

    cacheStore = StoreBuilder.<MediaLink, MediaLink>key()
        .fetcher(unresolvedMediaLink -> resolveFromNetwork(unresolvedMediaLink))
        .persister(new StoreFilePersister<>(cacheFileSystem, pathResolver, jsonParser))
        .open();
  }

  @NonNull
  private StoreFilePersister.JsonParser<MediaLink, MediaLink> createJsonParserForMediaLinkPersister(Moshi moshi) {
    return new StoreFilePersister.JsonParser<MediaLink, MediaLink>() {
      @Override
      public MediaLink fromJson(MediaLink key, BufferedSource jsonBufferedSource) throws IOException {
        Type expectedType = getExpectedValueClassFromKey(key);
        JsonAdapter<MediaLink> adapter = moshi.adapter(expectedType);
        return adapter.fromJson(jsonBufferedSource);
      }

      @Override
      public String toJson(MediaLink key, MediaLink value) {
        Type expectedType = getExpectedValueClassFromKey(key);
        JsonAdapter<MediaLink> adapter = moshi.adapter(expectedType);
        return adapter.toJson(value);
      }

      @NonNull
      private Type getExpectedValueClassFromKey(MediaLink key) {
        Type expectedType;
        if (key instanceof StreamableUnresolvedLink) {
          expectedType = StreamableLink.class;
        } else if (key instanceof ImgurAlbumUnresolvedLink){
          expectedType = ImgurAlbumLink.class;
        } else {
          throw new UnsupportedOperationException("Unknown key class type: " + key);
        }
        return expectedType;
      }
    };
  }

  /**
   * Remember to handle {@link ImgurApiRateLimitReachedException}.
   */
  public Single<MediaLink> resolveActualLinkIfNeeded(MediaLink unresolvedLink) {
    if (unresolvedLink instanceof UnresolvedMediaLink) {
      return cacheStore.get(unresolvedLink);
    } else {
      return Single.just(unresolvedLink);
    }
  }

  private Single<MediaLink> resolveFromNetwork(MediaLink unresolvedLink) {
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
              return ((MediaLink) UrlParser.parse(imgurResponse.images().get(0).url()));
            }
          });

    } else {
      return Single.just(unresolvedLink);
    }
  }

  private List<ImgurLink> convertImgurImagesToImgurMediaLinks(List<ImgurImage> imgurImages) {
    List<ImgurLink> imgurImageLinks = new ArrayList<>(imgurImages.size());
    for (ImgurImage imgurImage : imgurImages) {
      imgurImageLinks.add(UrlParser.createImgurLink(imgurImage.url(), imgurImage.title(), imgurImage.description()));
    }
    return imgurImageLinks;
  }

  public String findOptimizedQualityImageForDisplay(@Nullable Thumbnails redditSuppliedImages, int deviceDisplayWidth, String defaultImageUrl) {
    if (redditSuppliedImages != null && UrlParser.isImagePath(defaultImageUrl)) {
      return Commons.findOptimizedImage(redditSuppliedImages, deviceDisplayWidth);
    } else {
      return defaultImageUrl;
    }
  }
}
