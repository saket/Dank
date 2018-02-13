package me.saket.dank.ui.submission.adapter;

import android.support.annotation.Nullable;
import android.text.Html;

import net.dean.jraw.models.Thumbnails;

import java.util.NoSuchElementException;

import me.saket.dank.BuildConfig;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.UrlParser;
import timber.log.Timber;

public class ImageWithMultipleVariants {

  private Optional<Thumbnails> optionalRedditSuppliedImages;

  public static ImageWithMultipleVariants of(@Nullable Thumbnails redditSuppliedImages) {
    return new ImageWithMultipleVariants(redditSuppliedImages);
  }

  private ImageWithMultipleVariants(@Nullable Thumbnails redditSuppliedImages) {
    this.optionalRedditSuppliedImages = Optional.ofNullable(redditSuppliedImages);
  }

  public boolean isNonEmpty() {
    return optionalRedditSuppliedImages.isPresent();
  }

  /**
   * Find an image provided by Reddit that is the closest to <var>preferredWidth</var>.
   * Gives preference to higher-res thumbnails if multiple images have the same distance from the preferred width.
   */
  public String findNearestFor(int preferredWidth) {
    if (!optionalRedditSuppliedImages.isPresent()) {
      throw new NoSuchElementException("No reddit supplied images present");
    }

    Thumbnails redditSuppliedImages = optionalRedditSuppliedImages.get();
    Thumbnails.Image closestImage = redditSuppliedImages.getSource();
    int closestDifference = preferredWidth - redditSuppliedImages.getSource().getWidth();

    for (Thumbnails.Image redditCopy : redditSuppliedImages.getVariations()) {
      int differenceAbs = Math.abs(preferredWidth - redditCopy.getWidth());
      if (differenceAbs < Math.abs(closestDifference)
          // If another image is found with the same difference, choose the higher-res image.
          || differenceAbs == closestDifference && redditCopy.getWidth() > closestImage.getWidth())
      {
        closestDifference = preferredWidth - redditCopy.getWidth();
        closestImage = redditCopy;
      }
    }

    // Reddit sends HTML-escaped URLs.
    //noinspection UnnecessaryLocalVariable
    String htmlEscapedUrl = Html.fromHtml(closestImage.getUrl()).toString();
    return htmlEscapedUrl;
  }

  public String findNearestFor(int preferredWidth, String defaultValue) {
    if (BuildConfig.DEBUG && UrlParser.isGifUrl(defaultValue)) {
      Timber.w("Optimizing GIFs is an error: %s", defaultValue);
      new Exception().printStackTrace();
    }

    if (optionalRedditSuppliedImages.isPresent()) {
      return findNearestFor(preferredWidth);
    } else {
      return defaultValue;
    }
  }
}
