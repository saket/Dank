package me.saket.dank.utils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableConverter;

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

  public static <T> Optional<T> optionallyFirst(List<T> items) {
    return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
  }
}
