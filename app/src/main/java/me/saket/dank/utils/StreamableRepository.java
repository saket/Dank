package me.saket.dank.utils;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import me.saket.dank.data.links.StreamableLink;
import me.saket.dank.di.Dank;

@Singleton
public class StreamableRepository {

  @Inject
  public StreamableRepository() {
  }

  // TODO: 01/04/17 Cache.
  public Single<StreamableLink> video(String videoId) {
    return Dank.api().streamableVideoDetails(videoId)
        .map(response -> {
          String videoUrl = response.url();
          String lowQualityVideoUrl = response.files().lowQualityVideo().url();
          String highQualityVideoUrl = response.files().highQualityVideo().url();
          return StreamableLink.create(videoUrl, lowQualityVideoUrl, highQualityVideoUrl);
        });
  }
}
