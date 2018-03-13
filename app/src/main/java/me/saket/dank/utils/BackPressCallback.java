package me.saket.dank.utils;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class BackPressCallback {

  public abstract boolean intercepted();

  public static BackPressCallback asIntercepted() {
    return new AutoValue_BackPressCallback(true);
  }

  public static BackPressCallback asIgnored() {
    return new AutoValue_BackPressCallback(false);
  }
}
