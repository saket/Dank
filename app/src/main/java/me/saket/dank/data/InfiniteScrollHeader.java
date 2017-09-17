package me.saket.dank.data;

import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import com.google.auto.value.AutoValue;

/**
 * Used in RecyclerView adapters that support infinite scrolling.
 */
@AutoValue
public abstract class InfiniteScrollHeader {

  public enum Type {
    PROGRESS,
    ERROR,
    HIDDEN,
    CUSTOM
  }

  public abstract Type type();

  @StringRes
  public abstract int titleRes();

  @DrawableRes
  public abstract int otherTypeIconRes();

  @ColorRes
  public abstract int otherTypeTextColor();

  public static InfiniteScrollHeader createHidden() {
    return new AutoValue_InfiniteScrollHeader(Type.HIDDEN, 0, 0, 0);
  }

  public static InfiniteScrollHeader createProgress(@StringRes int progressTitleRes) {
    return new AutoValue_InfiniteScrollHeader(Type.PROGRESS, progressTitleRes, 0, 0);
  }

  public static InfiniteScrollHeader createError(@StringRes int errorTitleRes) {
    return new AutoValue_InfiniteScrollHeader(Type.ERROR, errorTitleRes, 0, 0);
  }
}
