package me.thanel.dawn.linkunfurler.readers

import me.thanel.dawn.linkunfurler.LinkMetadata
import org.jsoup.nodes.Document
import java.net.MalformedURLException
import java.net.URL

class PlayStoreLinkMetadataReader : GenericLinkMetadataReader() {
  override fun extract(
    url: String,
    pageDocument: Document
  ): LinkMetadata? {
    if (!isGooglePlayLink(url)) {
      return null
    }

    val linkTitle = readTitle(pageDocument)
    val linkImage = readImageUrl(url, pageDocument)
    val faviconUrl = readFaviconUrl(url, pageDocument)
    return LinkMetadata(url, linkTitle, faviconUrl, linkImage)
  }

  @Throws(MalformedURLException::class)
  override fun readImageUrl(url: String, pageDocument: Document): String {
    // Play Store uses a shitty favicon so I'm using an empty favicon and this image.
    return "https://www.android.com/static/2016/img/icons/why-android/play_2x.png"
  }

  override fun fallbackFaviconUrl(url: String): String? {
    return null
  }

  companion object {

    private fun isGooglePlayLink(url: String): Boolean {
      return URL(url).host.contains("play.google")
    }
  }
}
