package me.saket.dank.ui.media

import io.reactivex.Single
import me.saket.dank.di.DankApi
import me.saket.dank.urlparser.StreamableLink
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamableRepository @Inject constructor(private val dankApi: DankApi) {

  fun video(videoId: String): Single<StreamableLink> {
    return dankApi.streamableVideoDetails(videoId)
        .map { response ->
          val highQualityVideo = response.files.highQualityVideo
          val highQualityVideoUrl = highQualityVideo.url()
          val lowQualityVideoUrl = response.files.lowQualityVideo?.url() ?: highQualityVideoUrl
          StreamableLink.create(response.url(), highQualityVideoUrl, lowQualityVideoUrl)
        }
  }
}
