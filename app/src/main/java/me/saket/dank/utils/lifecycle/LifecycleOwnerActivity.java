package me.saket.dank.utils.lifecycle;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public abstract class LifecycleOwnerActivity extends AppCompatActivity implements LifecycleOwner {

  private static final Object INSTANCE = new Object();

  private Subject<Object> onCreates = PublishSubject.create();
  private Subject<Object> onStarts = PublishSubject.create();
  private Subject<Object> onResumes = PublishSubject.create();
  private Subject<Object> onPauses = PublishSubject.create();
  private Subject<Object> onStops = PublishSubject.create();
  private Subject<Object> onDestroys = PublishSubject.create();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    onCreates.onNext(INSTANCE);
  }

  @Override
  protected void onStart() {
    super.onStart();
    onStarts.onNext(INSTANCE);
  }

  @Override
  protected void onResume() {
    super.onResume();
    onResumes.onNext(INSTANCE);
  }

  @Override
  protected void onPause() {
    onPauses.onNext(INSTANCE);
    super.onPause();
  }

  @Override
  protected void onStop() {
    onStops.onNext(INSTANCE);
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    onDestroys.onNext(INSTANCE);
    super.onDestroy();
  }

  public Observable<Object> onCreates() {
    return onCreates;
  }

  public Observable<Object> onStarts() {
    return onStarts;
  }

  public Observable<Object> onResumes() {
    return onResumes;
  }

  public Observable<Object> onPauses() {
    return onPauses;
  }

  public Observable<Object> onStops() {
    return onStops;
  }

  public Observable<Object> onDestroys() {
    return onDestroys;
  }
}
