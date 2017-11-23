package me.saket.dank.data;

import android.content.Intent;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ActivityResult {

  public abstract int requestCode();

  public abstract int resultCode();

  public abstract Intent data();

  public static ActivityResult create(int requestCode, int resultCode, Intent data) {
    return new AutoValue_ActivityResult(requestCode, resultCode, data);
  }
}
