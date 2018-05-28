package me.saket.dank.data

import me.saket.dank.di.RootModule
import org.junit.Test
import java.io.IOException

class StreamableVideoResponseJsonTest {

  @Test
  @Throws(IOException::class)
  fun `de-serializing a streamable response with null low quality video should not fail`() {
    val moshi = RootModule.provideMoshi()
    val adapter = moshi.adapter(StreamableVideoResponse::class.java)

    val response = StreamableVideoResponse(
        files = StreamableVideoResponse.Files(
            highQualityVideo = StreamableVideoResponse.HighQualityVideo("high-quality"),
            lowQualityVideo = null),
        urlWithoutProtocol = "streamable.com/fxn88")

    val json = adapter.toJson(response)
    val parsed = adapter.fromJson(json)
    assert(parsed == response)
  }
}
