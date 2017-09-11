package me.saket.dank.utils.lifecycle;

import io.reactivex.Observable;

public interface LifecycleOwner {

  Observable<Object> onCreates();

  Observable<Object> onStarts();

  Observable<Object> onResumes();

  Observable<Object> onPauses();

  Observable<Object> onStops();

  Observable<Object> onDestroys();
}
