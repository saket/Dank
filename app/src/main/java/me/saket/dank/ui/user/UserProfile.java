package me.saket.dank.ui.user;

import com.google.auto.value.AutoValue;
import net.dean.jraw.models.Account;

@AutoValue
public abstract class UserProfile {

  public abstract Account data();

  public static UserProfile create(Account data) {
    return new AutoValue_UserProfile(data);
  }
}
