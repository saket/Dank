package me.saket.dank.utils;

import android.support.annotation.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility methods for arrays.
 */
public class Arrays2 {

  public static <T> T[] toArray(@Nullable Collection<?> collection, Class<T> tClass) {
    if (collection == null) {
      return null;
    }

    //noinspection unchecked
    T[] result = (T[]) Array.newInstance(tClass, collection.size());
    //noinspection SuspiciousToArrayCall
    result = collection.toArray(result);
    return result;
  }

  public static <T> List<T> concatenate(T firstItem, List<T> moreItems) {
    ArrayList<T> concatenatedItems = new ArrayList<>(1 + moreItems.size());
    concatenatedItems.add(firstItem);
    concatenatedItems.addAll(moreItems);
    return concatenatedItems;
  }
}
