package me.saket.dank.utils;

import android.content.res.Resources;

import androidx.annotation.StringRes;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class StringOrResource {

  public abstract Optional<String> string();

  public abstract Optional<Integer> stringRes();

  public static StringOrResource of(String string) {
    return new AutoValue_StringOrResource(Optional.of(string), Optional.empty());
  }

  public static StringOrResource of(@StringRes int stringRes) {
    return new AutoValue_StringOrResource(Optional.empty(), Optional.of(stringRes));
  }

  public String get(Resources resources) {
    if (string().isPresent()) {
      return string().get();
    }

    return stringRes()
        .map(resId -> resources.getString(resId))
        .orElseThrow(() -> new AssertionError("Neither string nor resourceId present"));
  }
}
