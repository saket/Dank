package me.saket.dank.data.links;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class ImgurLink extends MediaLink implements Parcelable {

  public abstract String unparsedUrl();

  @Nullable
  public abstract String title();

  @Nullable
  public abstract String description();

  @Override
  public abstract String highQualityUrl();

  @Override
  public abstract String lowQualityUrl();

  @Memoized
  @Override
  public Link.Type type() {
    return highQualityUrl().endsWith("mp4") ? Type.SINGLE_VIDEO : Type.SINGLE_IMAGE_OR_GIF;
  }

  public static ImgurLink create(String unparsedUrl, String title, String description, String imageUrl) {
    return new AutoValue_ImgurLink(unparsedUrl, title, description, imageUrl, imageUrl);
  }

  public static JsonAdapter<ImgurLink> jsonAdapter(Moshi moshi) {
    return new AutoValue_ImgurLink.MoshiJsonAdapter(moshi);
  }
}
