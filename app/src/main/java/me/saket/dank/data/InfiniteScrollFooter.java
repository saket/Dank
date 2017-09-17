package me.saket.dank.data;

import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.View;

import com.google.auto.value.AutoValue;

/**
 * Used in RecyclerView adapters that support infinite scrolling.
 */
@AutoValue
public abstract class InfiniteScrollFooter {

  public enum Type {
    PROGRESS,
    ERROR,
    HIDDEN,
  }

  public abstract Type type();

  @StringRes
  public abstract int titleRes();

  @DrawableRes
  public abstract int otherTypeIconRes();

  @ColorRes
  public abstract int otherTypeTextColor();

  public static InfiniteScrollFooter createHidden() {
    return new AutoValue_InfiniteScrollFooter(Type.HIDDEN, 0, 0, 0);
  }

  public static InfiniteScrollFooter createProgress() {
    return new AutoValue_InfiniteScrollFooter(Type.PROGRESS, 0, 0, 0);
  }

  public static InfiniteScrollFooter createError(@StringRes int errorTitleRes) {
    return new AutoValue_InfiniteScrollFooter(Type.ERROR, errorTitleRes, 0, 0);
  }
}
