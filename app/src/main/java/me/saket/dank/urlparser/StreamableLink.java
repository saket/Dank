package me.saket.dank.urlparser;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import me.saket.dank.BuildConfig;
import me.saket.dank.utils.Urls;
import timber.log.Timber;

@AutoValue
public abstract class StreamableLink extends MediaLink implements Parcelable {

  @Override
  public abstract String unparsedUrl();

  @Override
  public Link.Type type() {
    return Link.Type.SINGLE_VIDEO;
  }

  @Override
  public abstract String highQualityUrl();

  @Override
  public abstract String lowQualityUrl();

  @Override
  public String cacheKey() {
    return cacheKeyWithClassName(Urls.parseFileNameWithExtension(highQualityUrl()));
  }

  public static StreamableLink create(String unparsedUrl, String highQualityVideoUrl, String lowQualityVideoUrl) {
    if (BuildConfig.DEBUG && (highQualityVideoUrl.startsWith("http://") || lowQualityVideoUrl.startsWith("http://"))) {
      Timber.e(new Exception("Use https for Streamable!"));
    }
    return new AutoValue_StreamableLink(unparsedUrl, highQualityVideoUrl, lowQualityVideoUrl);
  }

  public static JsonAdapter<StreamableLink> jsonAdapter(Moshi moshi) {
    return new AutoValue_StreamableLink.MoshiJsonAdapter(moshi);
  }
}
