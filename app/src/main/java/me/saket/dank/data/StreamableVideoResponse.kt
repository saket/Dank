package me.saket.dank.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import me.saket.dank.di.DankApi

/**
 * API response of [DankApi.streamableVideoDetails].
 */
@JsonClass(generateAdapter = true)
data class StreamableVideoResponse(

    @Json(name = "files")
    val files: Files,

    /**
     * Note: URL doesn't contain the scheme or '://'.
     * E.g.: 'streamable.com/fxn88'. Not sure if 'protocol' is the right word.
     */
    @Json(name = "url")
    val urlWithoutProtocol: String
) {

  fun url(): String {
    return "https://$urlWithoutProtocol"
  }

  @JsonClass(generateAdapter = true)
  data class Files(
      @Json(name = "mp4")
      val highQualityVideo: HighQualityVideo,

      /**
       * Low quality video is usually empty for new videos for which Streamable hasn't generated a lower quality yet.
       */
      @Json(name = "mp4-mobile")
      val lowQualityVideo: LowQualityVideo?
  )

  @JsonClass(generateAdapter = true)
  data class HighQualityVideo(@Json(name = "url") val schemeLessUrl: String) {

    /**
     * Sceheme-less because:
     * '//cdn-e2.streamable.com/video/mp4-mobile/fxn88.mp4?token=1492269114_8becb8f6fab5b53e5b0ee0f0c6a8f8969e38a04a'.
     */
    fun url(): String {
      return "https:$schemeLessUrl"
    }
  }

  /**
   * @param schemeLessUrl This is nullable because Streamable occasionally sends null low quality url :/
   * */
  @JsonClass(generateAdapter = true)
  data class LowQualityVideo(@Json(name = "url") val schemeLessUrl: String?) {

    fun url(): String? {
      return schemeLessUrl?.let { "https:$it" }
    }
  }
}
