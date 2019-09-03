package me.saket.dank.urlparser;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

/**
 * Meta-data of a URL.
 */
@AutoValue
public abstract class LinkMetadata {

  public abstract String url();

  @Nullable
  public abstract String title();

  @Nullable
  public abstract String faviconUrl();

  @Nullable
  public abstract String imageUrl();

  public boolean hasImage() {
    return !TextUtils.isEmpty(imageUrl());
  }

  public boolean hasFavicon() {
    return !TextUtils.isEmpty(faviconUrl());
  }

  public static LinkMetadata create(String url, @Nullable String title, @Nullable String faviconUrl, @Nullable String imageUrl) {
    return new AutoValue_LinkMetadata(url, title, faviconUrl, imageUrl);
  }

  public static JsonAdapter<LinkMetadata> jsonAdapter(Moshi moshi) {
    return new AutoValue_LinkMetadata.MoshiJsonAdapter(moshi);
  }
}
