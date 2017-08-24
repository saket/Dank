package me.saket.dank.utils;

import static java.util.Collections.unmodifiableList;

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

  @Inject
  public MediaHostRepository(StreamableRepository streamableRepository, ImgurRepository imgurRepository) {
    this.streamableRepository = streamableRepository;
    this.imgurRepository = imgurRepository;
  }

  /**
   * Remember to handle {@link ImgurApiRateLimitReachedException}.
   */
  // TODO: 01/04/17 Cache.
  public Single<? extends MediaLink> resolveActualLinkIfNeeded(MediaLink unresolvedLink) {
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
