package me.saket.dank.utils;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

/**
 * To avoid catching exceptions.
 */
public interface SafeConsumer<T> extends Consumer<T> {
    @Override
    void accept(@NonNull T t);
}
