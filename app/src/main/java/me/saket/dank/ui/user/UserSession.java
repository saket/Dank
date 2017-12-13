package me.saket.dank.ui.user;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class UserSession {

  public abstract String username();

  public static UserSession create(String username) {
    return new AutoValue_UserSession(username);
  }
}
