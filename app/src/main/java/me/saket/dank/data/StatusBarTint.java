package me.saket.dank.data;

import com.google.auto.value.AutoValue;

/**
 * Contains information of a color that can be used for tinting the status bar.
 */
@AutoValue
public abstract class StatusBarTint {

  public abstract int color();

  public abstract boolean isDarkColor();

  public abstract boolean delayedTransition();

  public static StatusBarTint create(int color, boolean isDarkColor) {
    return new AutoValue_StatusBarTint(color, isDarkColor, false);
  }

  public static StatusBarTint create(int color, boolean isDarkColor, boolean delayedTransition) {
    return new AutoValue_StatusBarTint(color, isDarkColor, delayedTransition);
  }

  public StatusBarTint withDelayedTransition(boolean delayedTransition) {
    return create(color(), isDarkColor(), delayedTransition);
  }
}
