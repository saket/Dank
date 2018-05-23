package me.saket.dank.urlparser;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import me.saket.dank.BuildConfig;
import me.saket.dank.utils.Urls;
import timber.log.Timber;

/**
 * Giphy.com for GIFs (converted to MP4s).
 */
@AutoValue
public abstract class GiphyLink extends MediaLink implements Parcelable {

  @Override
  public abstract String unparsedUrl();

  @Override
  public Link.Type type() {
    return Link.Type.SINGLE_VIDEO;
  }

  public abstract String highQualityUrl();

  public abstract String lowQualityUrl();

  @Override
  public String cacheKey() {
    return cacheKeyWithClassName(Urls.parseFileNameWithExtension(highQualityUrl()));
  }

  public static GiphyLink create(String unparsedUrl, String gifVideoUrl) {
    if (BuildConfig.DEBUG && gifVideoUrl.startsWith("http://")) {
      Timber.e(new Exception("Use https for giphy!"));
    }
    return new AutoValue_GiphyLink(unparsedUrl, gifVideoUrl, gifVideoUrl);
  }

  public static JsonAdapter<GiphyLink> jsonAdapter(Moshi moshi) {
    return new AutoValue_GiphyLink.MoshiJsonAdapter(moshi);
  }
}
