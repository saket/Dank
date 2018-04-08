package me.saket.dank.utils;

public interface SafeFunction<I, O> {

  O apply(I input);
}
