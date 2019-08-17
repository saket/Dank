package me.saket.dank.urlparser;

import android.os.Parcelable;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import me.saket.dank.BuildConfig;
import me.saket.dank.utils.Urls;
import timber.log.Timber;

@AutoValue
public abstract class ImgurLink extends MediaLink implements Parcelable {

  public abstract String unparsedUrl();

  @Override
  public abstract Link.Type type();

  @Nullable
  public abstract String title();

  @Nullable
  public abstract String description();

  @Override
  public abstract String highQualityUrl();

  @Override
  public abstract String lowQualityUrl();

  @Override
  public String cacheKey() {
    return cacheKeyWithClassName(Urls.parseFileNameWithExtension(highQualityUrl()));
  }

  public static ImgurLink create(String unparsedUrl, Type type, @Nullable String title, @Nullable String description, String imageUrl) {
    if (BuildConfig.DEBUG && imageUrl.startsWith("http://")) {
      Timber.e(new Exception("Use https for imgur!"));
    }
    return new AutoValue_ImgurLink(unparsedUrl, type, title, description, imageUrl, imageUrl);
  }

  public static JsonAdapter<ImgurLink> jsonAdapter(Moshi moshi) {
    return new AutoValue_ImgurLink.MoshiJsonAdapter(moshi);
  }
}
