package me.saket.dank.data;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ActivityResult {

  public abstract int requestCode();

  public abstract int resultCode();

  @Nullable
  public abstract Intent data();

  public boolean isResultOk() {
    return resultCode() == Activity.RESULT_OK;
  }

  public static ActivityResult create(int requestCode, int resultCode, @Nullable Intent data) {
    return new AutoValue_ActivityResult(requestCode, resultCode, data);
  }
}
