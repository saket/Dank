package me.saket.dank.ui;

import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ScreenSavedState implements Parcelable {

  @Nullable
  public abstract Parcelable superSavedState();

  public abstract Bundle values();

  public static ScreenSavedState combine(@Nullable Parcelable superSavedState, Bundle values) {
    return new AutoValue_ScreenSavedState(superSavedState, values);
  }
}
