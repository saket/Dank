package me.saket.dank.utils;

import android.os.Build;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableConverter;
import io.reactivex.functions.Function;

/**
 * Utility methods for arrays.
 */
public class Arrays2 {

  public static <T> T[] toArray(Collection<?> collection, Class<T> tClass) {
    //noinspection ConstantConditions
    if (collection == null) {
      throw new NullPointerException();
    }

    //noinspection unchecked
    T[] result = (T[]) Array.newInstance(tClass, collection.size());
    //noinspection SuspiciousToArrayCall
    result = collection.toArray(result);
    return result;
  }

  public static <T> ObservableConverter<List<T>, Observable<List<T>>> immutable() {
    return upstream -> upstream.map(items -> Collections.unmodifiableList(items));
  }

  public static <T> Function<List<T>, List<T>> toImmutable() {
    return list -> Collections.unmodifiableList(list);
  }

  public static <T> Optional<T> firstOrEmpty(List<T> items) {
    return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
  }

  public static <K, V> HashMap<K, V> hashMap(int initialCapacity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      return new HashMap<>(initialCapacity, 1f);
    } else {
      // Load factor always defaults to 0.75f on API 25 and lower.
      return new HashMap<>((int) Math.ceil((initialCapacity / 0.75)));
    }
  }
}
