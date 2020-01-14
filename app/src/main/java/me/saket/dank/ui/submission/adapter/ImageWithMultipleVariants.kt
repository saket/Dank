package me.saket.dank.ui.submission.adapter

import android.text.Html
import me.saket.dank.urlparser.UrlParser
import me.saket.dank.utils.Optional
import net.dean.jraw.models.SubmissionPreview
import java.util.*
import kotlin.math.abs

class ImageWithMultipleVariants private constructor(private val optionalRedditPreviews: Optional<SubmissionPreview>) {

  val isNonEmpty: Boolean
    get() = optionalRedditPreviews.isPresent

  /**
   * Find an image provided by Reddit that is the closest to <var>preferredWidth</var>.
   * Gives preference to higher-res thumbnails if multiple images have the same distance from the preferred width.
   */
  @Suppress("DEPRECATION")
  fun findNearestFor(preferredWidth: Int): String {
    if (optionalRedditPreviews.isEmpty) {
      throw NoSuchElementException("No reddit supplied images present")
    }

    val redditPreviews = optionalRedditPreviews.get().images[0]
    var closestImage: SubmissionPreview.Variation = redditPreviews.source
    var closestDifference = preferredWidth - redditPreviews.source.width

    for (variation in redditPreviews.resolutions) {
      val differenceAbs = abs(preferredWidth - variation.width)
      if (differenceAbs < abs(closestDifference)
          // If another image is found with the same difference, choose the higher-res image.
          || differenceAbs == closestDifference && variation.width > closestImage.width) {
        closestDifference = preferredWidth - variation.width
        closestImage = variation
      }
    }

    // Reddit sends HTML-escaped URLs.
    return Html.fromHtml(closestImage.url).toString()
  }

  fun findNearestFor(preferredWidth: Int, defaultValue: String): String {
    if (UrlParser.isGifUrl(defaultValue)) {
      throw AssertionError("Optimizing GIFs is an error: $defaultValue")
    }

    return if (optionalRedditPreviews.isPresent) {
      findNearestFor(preferredWidth)
    } else {
      defaultValue
    }
  }

  companion object {

    fun of(redditSuppliedImages: SubmissionPreview?): ImageWithMultipleVariants {
      return ImageWithMultipleVariants(Optional.ofNullable(redditSuppliedImages))
    }

    fun of(redditSuppliedImages: Optional<SubmissionPreview>): ImageWithMultipleVariants {
      return ImageWithMultipleVariants(redditSuppliedImages)
    }
  }
}
