package me.saket.dank.utils;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import me.saket.dank.data.MediaLink;

@Singleton
public class VideoHostRepository {

  private final StreamableRepository streamableRepository;

  @Inject
  public VideoHostRepository(StreamableRepository streamableRepository) {
    this.streamableRepository = streamableRepository;
  }

  // TODO: 01/04/17 Cache.
  public Single<? extends MediaLink> fetchActualVideoUrlIfNeeded(MediaLink unresolvedLink) {
    if (unresolvedLink instanceof MediaLink.StreamableUnresolved) {
      return streamableRepository.video(((MediaLink.StreamableUnresolved) unresolvedLink).videoId());
    } else {
      return Single.just(unresolvedLink);
    }
  }
}
