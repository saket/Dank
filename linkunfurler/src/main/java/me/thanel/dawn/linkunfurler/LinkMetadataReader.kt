package me.thanel.dawn.linkunfurler

import org.jsoup.nodes.Document

interface LinkMetadataReader {

  fun extract(url: String, pageDocument: Document): LinkMetadata?
}
