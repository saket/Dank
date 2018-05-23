package me.saket.dank.urlparser;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import me.saket.dank.BuildConfig;
import me.saket.dank.utils.Urls;
import timber.log.Timber;

/**
 * Gfycat.com for GIFs (converted to MP4).
 */
@AutoValue
public abstract class GfycatLink extends MediaLink implements Parcelable {

  @Override
  public abstract String unparsedUrl();

  public abstract String threeWordId();

  @Override
  public abstract String highQualityUrl();

  @Override
  public abstract String lowQualityUrl();

  @Override
  public String cacheKey() {
    return cacheKeyWithClassName(Urls.parseFileNameWithExtension(highQualityUrl()));
  }

  @Override
  public Link.Type type() {
    return Link.Type.SINGLE_VIDEO;
  }

  public static GfycatLink create(String unparsedUrl, String threeWordId, String highQualityVideoUrl, String lowQualityVideoUrl) {
    if (BuildConfig.DEBUG && (highQualityVideoUrl.startsWith("http://") || lowQualityVideoUrl.startsWith("http://"))) {
      Timber.e(new Exception("Use https for Gfycat!"));
    }
    return new AutoValue_GfycatLink(unparsedUrl, threeWordId, highQualityVideoUrl, lowQualityVideoUrl);
  }

  public static JsonAdapter<GfycatLink> jsonAdapter(Moshi moshi) {
    return new AutoValue_GfycatLink.MoshiJsonAdapter(moshi);
  }
}
