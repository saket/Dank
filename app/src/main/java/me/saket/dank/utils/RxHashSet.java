package me.saket.dank.utils;

import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.Relay;

import java.util.Collection;
import java.util.HashSet;

import io.reactivex.Observable;

public class RxHashSet<T> extends HashSet<T> {
  final Relay<Integer> changeEvents = BehaviorRelay.create();

  public RxHashSet() {
    changeEvents.accept(size());
  }

  public RxHashSet(int initialCapacity) {
    super(initialCapacity);
    changeEvents.accept(size());
  }

  public Observable<Integer> changes() {
    return changeEvents;
  }

  @Override
  public boolean add(T t) {
    boolean result = super.add(t);
    changeEvents.accept(size());
    return result;
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    boolean result = super.addAll(c);
    changeEvents.accept(size());
    return result;
  }

  @Override
  public boolean remove(Object o) {
    boolean result = super.remove(o);
    changeEvents.accept(size());
    return result;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    boolean result = super.removeAll(c);
    changeEvents.accept(size());
    return result;
  }
}
