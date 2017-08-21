package me.saket.dank.ui.user;

import android.support.annotation.Nullable;
import com.google.auto.value.AutoValue;
import net.dean.jraw.models.Account;

@AutoValue
public abstract class UserProfile {

  public abstract Account account();

  @Nullable
  public abstract UserSubreddit userSubreddit();

  public static UserProfile create(Account data, @Nullable UserSubreddit userSubreddit) {
    return new AutoValue_UserProfile(data, userSubreddit);
  }
}
