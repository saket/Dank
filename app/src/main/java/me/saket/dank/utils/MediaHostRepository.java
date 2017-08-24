package me.saket.dank.utils;

import static java.util.Collections.unmodifiableList;

import com.nytimes.android.external.store3.base.impl.Store;
import com.nytimes.android.external.store3.base.impl.StoreBuilder;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import me.saket.dank.data.MediaLink;
import me.saket.dank.data.exceptions.ImgurApiRateLimitReachedException;

@Singleton
public class MediaHostRepository {

  private final StreamableRepository streamableRepository;
  private final ImgurRepository imgurRepository;
  private final Store<MediaLink, MediaLink> cacheStore;

  @Inject
  public MediaHostRepository(StreamableRepository streamableRepository, ImgurRepository imgurRepository) {
    this.streamableRepository = streamableRepository;
    this.imgurRepository = imgurRepository;

    cacheStore = StoreBuilder.<MediaLink, MediaLink>key()
        .fetcher(unresolvedMediaLink -> resolveFromNetwork(unresolvedMediaLink))
        .open();
  }

  /**
   * Remember to handle {@link ImgurApiRateLimitReachedException}.
   */
  public Single<MediaLink> resolveActualLinkIfNeeded(MediaLink unresolvedLink) {
    if (unresolvedLink instanceof MediaLink.StreamableUnresolved || unresolvedLink instanceof MediaLink.ImgurUnresolvedGallery) {
      return cacheStore.get(unresolvedLink);
    } else {
      return Single.just(unresolvedLink);
    }
  }

  private Single<MediaLink> resolveFromNetwork(MediaLink unresolvedLink) {
    if (unresolvedLink instanceof MediaLink.StreamableUnresolved) {
      return streamableRepository.video(((MediaLink.StreamableUnresolved) unresolvedLink).videoId());

    } else if (unresolvedLink instanceof MediaLink.ImgurUnresolvedGallery) {
      return imgurRepository.gallery(((MediaLink.ImgurUnresolvedGallery) unresolvedLink))
          .map(imgurResponse -> {
            if (imgurResponse.isAlbum()) {
              String albumUrl = ((MediaLink.ImgurUnresolvedGallery) unresolvedLink).albumUrl();
              List<MediaLink.Imgur> images = unmodifiableList(imgurRepository.convertImgurImagesToImgurMediaLinks(imgurResponse.images()));
              return MediaLink.ImgurAlbum.create(albumUrl, imgurResponse.albumTitle(), imgurResponse.albumCoverImageUrl(), images);

            } else {
              // Single image.
              return ((MediaLink) UrlParser.parse(imgurResponse.images().get(0).url()));
            }
          });

    } else {
      return Single.just(unresolvedLink);
    }
  }
}
