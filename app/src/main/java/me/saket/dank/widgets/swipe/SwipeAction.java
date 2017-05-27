package me.saket.dank.widgets.swipe;

import android.support.annotation.ColorInt;

import com.google.auto.value.AutoValue;

/**
 * Marks the release area for a swipe action.
 */
@AutoValue
public abstract class SwipeAction {

  public abstract String name();

  public abstract int backgroundColor();

  public abstract float layoutWeight();

  public static SwipeAction create(String name, @ColorInt int backgroundColor, float layoutWeight) {
    return new AutoValue_SwipeAction(name, backgroundColor, layoutWeight);
  }
}
