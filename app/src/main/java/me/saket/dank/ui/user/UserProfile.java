package me.saket.dank.ui.user;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import net.dean.jraw.models.Account;

@AutoValue
public abstract class UserProfile {

  public abstract Account account();

  @Nullable
  public abstract UserSubreddit userSubreddit();

  public static UserProfile create(Account data) {
    //noinspection ConstantConditions
    return new AutoValue_UserProfile(data, null);
  }

  public static UserProfile create(Account data, @Nullable UserSubreddit userSubreddit) {
    //noinspection ConstantConditions
    return new AutoValue_UserProfile(data, userSubreddit);
  }

  public static JsonAdapter<UserProfile> jsonAdapter(Moshi moshi) {
    return new AutoValue_UserProfile.MoshiJsonAdapter(moshi);
  }
}
