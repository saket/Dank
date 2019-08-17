package me.saket.dank.utils;

import androidx.recyclerview.widget.DiffUtil;

import java.util.Collections;
import java.util.List;

import io.reactivex.FlowableTransformer;
import io.reactivex.functions.BiFunction;

public class RxDiffUtil {

  public static <T> FlowableTransformer<List<T>, Pair<List<T>, DiffUtil.DiffResult>> calculate(
      DiffUtil.ItemCallback<T> itemCallback
  ) {
    Pair<List<T>, DiffUtil.DiffResult> initialPair = Pair.createNullable(Collections.emptyList(), null);
    return upstream -> upstream
        .scan(initialPair, (latestPair, nextItems) -> {
          ListDiffUtilCallbacks<T> callback = new ListDiffUtilCallbacks<>(latestPair.first(), nextItems, itemCallback);
          DiffUtil.DiffResult result = DiffUtil.calculateDiff(callback, true);
          return Pair.create(nextItems, result);
        })
        .skip(1); // Initial value is dummy.
  }

  // FIXME rename to calculate().
  public static <T> FlowableTransformer<List<T>, Pair<List<T>, DiffUtil.DiffResult>> calculateDiff(
      BiFunction<List<T>, List<T>, SimpleDiffUtilsCallbacks<T>> diffCallbacks)
  {
    Pair<List<T>, DiffUtil.DiffResult> initialPair = Pair.createNullable(Collections.emptyList(), null);
    return upstream -> upstream
        //.doOnNext(o -> {
        //  boolean isImmutable = false;
        //  try {
        //    if (!o.isEmpty()) {
        //      o.remove(0);
        //    }
        //  } catch (UnsupportedOperationException expected) {
        //    isImmutable = true;
        //  }
        //
        //  if (!isImmutable) {
        //    throw new AssertionError("Non immutable list");
        //  }
        //})
        .scan(initialPair, (latestPair, nextItems) -> {
          DiffUtil.Callback callback = diffCallbacks.apply(latestPair.first(), nextItems);
          DiffUtil.DiffResult result = DiffUtil.calculateDiff(callback, true);
          return Pair.create(nextItems, result);
        })
        .skip(1);  // Initial value is dummy.
  }
}
