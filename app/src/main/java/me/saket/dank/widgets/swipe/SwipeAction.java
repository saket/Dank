package me.saket.dank.widgets.swipe;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;

import com.google.auto.value.AutoValue;

/**
 * Marks the release area for a swipe action.
 */
@AutoValue
public abstract class SwipeAction {

  @StringRes
  public abstract int labelRes();

  @ColorRes
  public abstract int backgroundColorRes();

  public abstract float layoutWeight();

  public static SwipeAction create(@StringRes int labelRes, @ColorRes int backgroundColorRes, float layoutWeight) {
    return new AutoValue_SwipeAction(labelRes, backgroundColorRes, layoutWeight);
  }
}
