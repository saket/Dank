package me.saket.dank.data.links;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

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

  public static ImgurLink create(String unparsedUrl, Type type, @Nullable String title, @Nullable String description, String imageUrl) {
    return new AutoValue_ImgurLink(unparsedUrl, type, title, description, imageUrl, imageUrl);
  }

  public static JsonAdapter<ImgurLink> jsonAdapter(Moshi moshi) {
    return new AutoValue_ImgurLink.MoshiJsonAdapter(moshi);
  }
}
