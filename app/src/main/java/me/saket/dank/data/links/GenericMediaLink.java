package me.saket.dank.data.links;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

/**
 * Direct link to a media hosted by an unknown/unsupported-yet website.
 */
@AutoValue
public abstract class GenericMediaLink extends MediaLink implements Parcelable {

  @Override
  public abstract String unparsedUrl();

  @Override
  public abstract Link.Type type();

  @Override
  public String highQualityUrl() {
    return unparsedUrl();
  }

  @Override
  public String lowQualityUrl() {
    return unparsedUrl();
  }

  public static GenericMediaLink create(String unparsedUrl, Link.Type type) {
    return new AutoValue_GenericMediaLink(unparsedUrl, type);
  }

  public static JsonAdapter<GenericMediaLink> jsonAdapter(Moshi moshi) {
    return new AutoValue_GenericMediaLink.MoshiJsonAdapter(moshi);
  }
}
