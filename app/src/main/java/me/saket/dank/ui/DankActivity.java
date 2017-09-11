package me.saket.dank.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import me.saket.dank.R;

/**
 * Base class for all activities.
 */
public abstract class DankActivity extends AppCompatActivity {

  private CompositeDisposable onStopDisposables;
  private CompositeDisposable onDestroyDisposables;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // If any Activity goes immersive, we don't want the system Ui of the background activity
    // to get adjusted. Adding this flag keeps them permanent.
    int flag = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
    getWindow().getDecorView().setSystemUiVisibility(flag);
  }

  @Override
  public void onStop() {
    if (onStopDisposables != null) {
      onStopDisposables.clear();
    }

    super.onStop();
  }

  @Override
  public void onDestroy() {
    if (onDestroyDisposables != null) {
      onDestroyDisposables.clear();
    }
    super.onDestroy();
  }

  protected void unsubscribeOnStop(Disposable subscription) {
    if (onStopDisposables == null) {
      onStopDisposables = new CompositeDisposable();
    }
    onStopDisposables.add(subscription);
  }

  protected void unsubscribeOnDestroy(Disposable disposable) {
    if (onDestroyDisposables == null) {
      onDestroyDisposables = new CompositeDisposable();
    }
    onDestroyDisposables.add(disposable);
  }

  protected void findAndSetupToolbar() {
    setSupportActionBar(ButterKnife.findById(this, R.id.toolbar));
  }

  /**
   * Because suppressing the null check everywhere is stupid.
   */
  @NonNull
  @Override
  public ActionBar getSupportActionBar() {
    //noinspection ConstantConditions
    return super.getSupportActionBar();
  }
}
