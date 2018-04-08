package me.saket.dank.utils;

/**
 * Because Func0 no longer exists in RxJava2.
 *
 * TODO: Convert to Supplier.
 */
public interface Function0<T> {
  T calculate();
}
