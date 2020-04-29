package me.thanel.dawn.linkunfurler.readers

import me.thanel.dawn.linkunfurler.LinkMetadata
import me.thanel.dawn.linkunfurler.LinkMetadataReader
import org.jsoup.nodes.Document
import java.net.URL

open class GenericLinkMetadataReader : LinkMetadataReader {

  override fun extract(url: String, pageDocument: Document): LinkMetadata? {
    // Websites seem to give better images for twitter, maybe because Twitter shows smaller
    // thumbnails than Facebook. So we'll prefer Twitter over Facebook.
    val linkTitle = readTitle(pageDocument)
    val linkImage = readImageUrl(url, pageDocument)
    val faviconUrl = readFaviconUrl(url, pageDocument)
    return LinkMetadata(url, linkTitle, faviconUrl, linkImage)
  }

  protected fun readTitle(pageDocument: Document): String? {
    var linkTitle = metaTag(pageDocument, "twitter:title", false)
    if (isEmpty(linkTitle)) {
      linkTitle = metaTag(pageDocument, "og:title", false)
    }
    if (isEmpty(linkTitle)) {
      linkTitle = pageDocument.title()
    }
    return linkTitle
  }

  protected open fun readImageUrl(url: String, pageDocument: Document): String? {
    var linkImage = metaTag(pageDocument, "twitter:image", true)
    if (isEmpty(linkImage)) {
      linkImage = metaTag(pageDocument, "og:image", true)
    }
    if (isEmpty(linkImage)) {
      linkImage = metaTag(pageDocument, "twitter:image:src", true)
    }
    if (isEmpty(linkImage)) {
      linkImage = metaTag(pageDocument, "og:image:secure_url", true)
    }
    // So... scheme-less URLs are also a thing.
    if (linkImage != null && linkImage.startsWith("//")) {
      val imageUrl = URL(url)
      linkImage = imageUrl.protocol + linkImage
    }
    return linkImage
  }

  protected fun readFaviconUrl(url: String, pageDocument: Document): String? {
    var faviconUrl = linkRelTag(pageDocument, "apple-touch-icon")
    if (isEmpty(faviconUrl)) {
      faviconUrl = linkRelTag(pageDocument, "apple-touch-icon-precomposed")
    }
    if (isEmpty(faviconUrl)) {
      faviconUrl = linkRelTag(pageDocument, "shortcut icon")
    }
    if (isEmpty(faviconUrl)) {
      faviconUrl = linkRelTag(pageDocument, "icon")
    }
    if (isEmpty(faviconUrl)) {
      faviconUrl = fallbackFaviconUrl(url)
    }
    return faviconUrl
  }

  protected open fun fallbackFaviconUrl(url: String): String? {
    // Thanks Google for the backup!
    return "https://www.google.com/s2/favicons?domain_url=$url"
  }

  companion object {

    private fun metaTag(document: Document, attr: String, useAbsoluteUrl: Boolean): String? {
      var elements = document.select("meta[name=$attr]")
      for (element in elements) {
        val url = element.attr(if (useAbsoluteUrl) "abs:content" else "content")
        if (url != null) {
          return url
        }
      }
      elements = document.select("meta[property=$attr]")
      for (element in elements) {
        val url = element.attr(if (useAbsoluteUrl) "abs:content" else "content")
        if (url != null) {
          return url
        }
      }
      return null
    }

    private fun linkRelTag(document: Document, rel: String): String? {
      val elements = document.head().select("link[rel=$rel]")
      if (elements.isEmpty()) {
        return null
      }
      var largestSizeUrl = elements.first().attr("abs:href")
      var largestSize = 0
      for (element in elements) { // Some websites have multiple icons for different sizes. Find the largest one.
        val sizes = element.attr("sizes")
        var size: Int
        if (sizes.contains("x")) {
          size = sizes.split("x").toTypedArray()[0].toInt()
          if (size > largestSize) {
            largestSize = size
            largestSizeUrl = element.attr("abs:href")
          }
        }
      }
      return largestSizeUrl
    }

    private fun isEmpty(string: String?): Boolean {
      return string == null || string.isEmpty()
    }
  }
}
