package me.saket.dank.ui.media;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import me.saket.dank.data.StreamableVideoResponse;
import me.saket.dank.di.DankApi;
import me.saket.dank.urlparser.StreamableLink;

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
          StreamableVideoResponse.Video highQualityVideo = response.files().highQualityVideo();
          String highQualityVideoUrl = highQualityVideo.url().get();

          // Low quality video is usually empty for new videos for which Streamable hasn't
          // generated a lower quality yet. Sometimes Streamable also sends empty low quality url :/
          String lowQualityVideoUrl = response.files()
              .lowQualityVideo()
              .flatMap(video -> video.url())
              .orElse(highQualityVideoUrl);

          String videoUrl = response.url();
          return StreamableLink.create(videoUrl, highQualityVideoUrl, lowQualityVideoUrl);
        });
  }
}
