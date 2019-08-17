package me.saket.dank.utils;

import androidx.annotation.NonNull;

import com.f2prateek.rx.preferences2.Preference;

public class RxPreferencesEnumTypeAdapter<T extends Enum<T>> implements Preference.Converter<T> {
  private final Class<T> enumClass;

  public RxPreferencesEnumTypeAdapter(Class<T> enumClass) {
    this.enumClass = enumClass;
  }

  @NonNull
  @Override
  public T deserialize(@NonNull String serialized) {
    return T.valueOf(enumClass, serialized);
  }

  @NonNull
  @Override
  public String serialize(@NonNull T value) {
    return value.name();
  }
}
