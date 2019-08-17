package me.saket.dank.data;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import android.view.View;

import com.google.auto.value.AutoValue;

/**
 * Used in RecyclerView adapters that support infinite scrolling.
 */
@AutoValue
@Deprecated
public abstract class InfiniteScrollHeaderFooter {

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

  @Nullable
  public abstract View.OnClickListener onClickListener();

  public static InfiniteScrollHeaderFooter createHidden() {
    return new AutoValue_InfiniteScrollHeaderFooter(Type.HIDDEN, 0, 0, 0, null);
  }

  public static InfiniteScrollHeaderFooter createFooterProgress() {
    return new AutoValue_InfiniteScrollHeaderFooter(Type.PROGRESS, 0, 0, 0, null);
  }

  public static InfiniteScrollHeaderFooter createError(@StringRes int errorTitleRes, View.OnClickListener onRetryClickListener) {
    return new AutoValue_InfiniteScrollHeaderFooter(Type.ERROR, errorTitleRes, 0, 0, onRetryClickListener);
  }
}
