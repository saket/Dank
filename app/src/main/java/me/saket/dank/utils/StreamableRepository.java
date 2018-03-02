package me.saket.dank.utils;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import me.saket.dank.data.StreamableVideoResponse;
import me.saket.dank.data.links.StreamableLink;
import me.saket.dank.di.DankApi;
import timber.log.Timber;

@Singleton
public class StreamableRepository {

  private final DankApi dankApi;

  @Inject
  public StreamableRepository(DankApi dankApi) {
    this.dankApi = dankApi;
  }

  public Single<StreamableLink> video(String videoId) {
    return dankApi.streamableVideoDetails(videoId)
        .map(response -> {
          Timber.i("response: %s", response);

          StreamableVideoResponse.Video highQualityVideo = response.files().highQualityVideo();
          String highQualityVideoUrl = highQualityVideo.url();

          // Low quality video is usually empty for new videos for which Streamable hasn't
          // generated a lower quality yet.
          String lowQualityVideoUrl = response.files().lowQualityVideo()
              .orElse(highQualityVideo)
              .url();

          String videoUrl = response.url();
          return StreamableLink.create(videoUrl, lowQualityVideoUrl, highQualityVideoUrl);
        });
  }
}
