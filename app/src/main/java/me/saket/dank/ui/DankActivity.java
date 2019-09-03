package me.saket.dank.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import me.saket.dank.R;
import me.saket.dank.utils.lifecycle.LifecycleOwnerActivity;

/**
 * Base class for all activities.
 */
public abstract class DankActivity extends LifecycleOwnerActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // If any Activity goes immersive, we don't want the system Ui of the background activity
    // to get adjusted. Adding this flag keeps them permanent.
    int flag = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
    getWindow().getDecorView().setSystemUiVisibility(flag);
  }

  protected void findAndSetupToolbar() {
    setSupportActionBar(findViewById(R.id.toolbar));
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

  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
  }
}
