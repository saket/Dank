package me.saket.dank.utils;

import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.f2prateek.rx.preferences2.Preference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RxPreferencesEnumListTypeAdapter<T extends Enum<T>> implements Preference.Converter<List<T>> {
  private final Class<T> enumClass;

  public RxPreferencesEnumListTypeAdapter(Class<T> enumClass) {
    this.enumClass = enumClass;
  }

  @NonNull
  @Override
  public List<T> deserialize(@NonNull String serialized) {
    // Check if serialized value is empty to return a list without any elements instead of trying to parse
    // empty value.
    if (serialized.isEmpty()) {
      return Collections.emptyList();
    }

    String[] values = serialized.split("\\|");
    ArrayList<T> enums = new ArrayList<>(values.length);
    for (String value : values) {
      enums.add(T.valueOf(enumClass, value));
    }
    return enums;
  }

  @NonNull
  @Override
  public String serialize(@NonNull List<T> enums) {
    ArrayList<String> names = new ArrayList<>(enums.size());
    for (T singleEnum : enums) {
      names.add(singleEnum.name());
    }
    return TextUtils.join("|", names);
  }
}
