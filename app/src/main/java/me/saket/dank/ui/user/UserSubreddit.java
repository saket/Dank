package me.saket.dank.ui.user;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

/**
 * Holds information about Reddit's new "profile pages", which are somewhat like user's
 * own subreddit with a banner image, profile image and a bio.
 */
@AutoValue
public abstract class UserSubreddit {

  @Json(name = "banner_img")
  public abstract String bannerImageUrl();

  @Json(name = "icon_img")
  public abstract String profileImageUrl();

  @Json(name = "title")
  public abstract String displayName();

  @Json(name = "public_description")
  public abstract String bio();

  public static JsonAdapter<UserSubreddit> jsonAdapter(Moshi moshi) {
    return new AutoValue_UserSubreddit.MoshiJsonAdapter(moshi);
  }
}
