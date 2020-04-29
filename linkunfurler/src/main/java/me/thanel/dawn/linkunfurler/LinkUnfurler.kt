package me.thanel.dawn.linkunfurler

import io.reactivex.Single
import me.thanel.dawn.linkunfurler.readers.GenericLinkMetadataReader
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Parses meta-data for URLs.
 */
class LinkUnfurler(
  private val genericMetadataReader: GenericLinkMetadataReader,
  private val customMetadataReaders: List<LinkMetadataReader>
) {

  /**
   * @param ignoreSocialMetadata When true, facebook/twitter titles, images will be ignored and the
   * page HTML title will instead be used.
   */
  fun unfurl(url: String, ignoreSocialMetadata: Boolean): Single<Result<LinkMetadata>> {
    val parsedURL = URL(url)
    val furledLinkMetadata = LinkMetadata(
      url = url,
      title = url,
      faviconUrl = null,
      imageUrl = null
    )
    if (!isHtmlPage(parsedURL)) {
      return Single.just(Result.success(furledLinkMetadata))
    }

    val downloadDocument =
      Single.fromCallable {
        val document = Jsoup.connect(url)
          .timeout(PAGE_DOWNLOAD_TIMEOUT_MILLIS)
          .get()
        metaRedirectIfNeeded(url, document)
      }

    val unfurledLinkFromNetwork = downloadDocument
      .map { parsedDocument ->
        extractMetadata(url, parsedDocument, ignoreSocialMetadata)
      }

    return unfurledLinkFromNetwork
      .map { Result.success(it) }
      .onErrorReturn { Result.failure(it) }
  }

  /**
   * Jsoup seems handle 301-redirects fine, but not 302s. This method manually does that.
   * Useful for websites like https://youtu.be/VJOAxlsMEJg.
   */
  private fun metaRedirectIfNeeded(url: String, document: Document): Document {
    val uri = URI.create(url)
    for (refresh in document.select("html head meta[http-equiv=refresh]")) {
      val matcher = REDIRECT_URL_PATTERN.matcher(refresh.attr("content"))
      if (matcher.matches()) {
        val redirectUrlMatch = matcher.group(1)
        if (redirectUrlMatch != null) {
          val redirectUrl = uri.resolve(redirectUrlMatch).toString()
          return Jsoup.connect(redirectUrl).get()
        }
      }
    }
    return document
  }

  private fun isHtmlPage(parsedURL: URL): Boolean {
    val extensionIndex = parsedURL.path.lastIndexOf('.')
    if (extensionIndex == -1) {
      return true
    }
    val urlExtension = parsedURL.path.substring(extensionIndex)
    return KNOWN_HTML_EXTENSIONS.contains(urlExtension)
  }

  private fun extractMetadata(
    url: String,
    pageDocument: Document,
    ignoreSocialMetadata: Boolean
  ): LinkMetadata {
    if (ignoreSocialMetadata) {
      return LinkMetadata(url, pageDocument.title(), null, null)
    }
    val metadataReaders = customMetadataReaders + genericMetadataReader
    for (reader in metadataReaders) {
      val metadata = reader.extract(url, pageDocument)
      if (metadata != null) {
        return metadata
      }
    }
    throw AssertionError("Couldn't read metadata")
  }

  companion object {
    private val PAGE_DOWNLOAD_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(30).toInt()
    private val KNOWN_HTML_EXTENSIONS = setOf("", "html", "htm", "aspx", "php")
    private val REDIRECT_URL_PATTERN = Pattern.compile("(?si)\\d+;\\s*url=(.+)|\\d+")
  }
}
