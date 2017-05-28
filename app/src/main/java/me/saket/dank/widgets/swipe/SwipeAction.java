package me.saket.dank.widgets.swipe;

import android.support.annotation.ColorRes;

import com.google.auto.value.AutoValue;

/**
 * Marks the release area for a swipe action.
 */
@AutoValue
public abstract class SwipeAction {

  public abstract String name();

  @ColorRes
  public abstract int backgroundColorRes();

  public abstract float layoutWeight();

  public static SwipeAction create(String name, @ColorRes int backgroundColorRes, float layoutWeight) {
    return new AutoValue_SwipeAction(name, backgroundColorRes, layoutWeight);
  }
}
