package me.saket.dank.utils;

import static java.util.Collections.unmodifiableList;

import android.support.annotation.Nullable;

import com.nytimes.android.external.store3.base.impl.Store;
import com.nytimes.android.external.store3.base.impl.StoreBuilder;

import net.dean.jraw.models.Thumbnails;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import me.saket.dank.data.ImgurImage;
import me.saket.dank.data.exceptions.ImgurApiRateLimitReachedException;
import me.saket.dank.data.links.ImgurAlbumUnresolvedLink;
import me.saket.dank.data.links.MediaAlbumLink;
import me.saket.dank.data.links.MediaLink;
import me.saket.dank.data.links.StreamableUnresolvedLink;
import me.saket.dank.data.links.UnresolvedMediaLink;

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
              List<MediaLink> images = unmodifiableList(convertImgurImagesToImgurMediaLinks(imgurResponse.images()));
              return MediaAlbumLink.create(albumUrl, imgurResponse.albumTitle(), imgurResponse.albumCoverImageUrl(), images);

            } else {
              // Single image.
              return ((MediaLink) UrlParser.parse(imgurResponse.images().get(0).url()));
            }
          });

    } else {
      return Single.just(unresolvedLink);
    }
  }

  private List<MediaLink> convertImgurImagesToImgurMediaLinks(List<ImgurImage> imgurImages) {
    List<MediaLink> imgurImageLinks = new ArrayList<>(imgurImages.size());
    for (ImgurImage imgurImage : imgurImages) {
      imgurImageLinks.add(UrlParser.createImgurLink(imgurImage.url(), imgurImage.title(), imgurImage.description()));
    }
    return imgurImageLinks;
  }

  public String findOptimizedQualityImageForDevice(String defaultImageUrl, @Nullable Thumbnails redditSuppliedImages, int deviceDisplayWidth) {
    boolean isStaticImage = UrlParser.isImagePath(defaultImageUrl);
    return isStaticImage
        ? Commons.findOptimizedImage(redditSuppliedImages, deviceDisplayWidth)
        : defaultImageUrl;
  }
}
