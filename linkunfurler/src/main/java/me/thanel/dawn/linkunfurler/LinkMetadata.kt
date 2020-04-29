package me.thanel.dawn.linkunfurler

data class LinkMetadata(
  val url: String,
  val title: String?,
  val faviconUrl: String?,
  val imageUrl: String?
) {

  fun hasImage(): Boolean = !imageUrl.isNullOrEmpty()

  fun hasFavicon(): Boolean = !faviconUrl.isNullOrEmpty()
}
