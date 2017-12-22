package me.saket.dank.ui.submission.adapter;

import android.graphics.Bitmap;
import android.support.annotation.ColorRes;

import com.google.auto.value.AutoValue;

import me.saket.dank.utils.Optional;

@AutoValue
public abstract class SubmissionContentLinkUiModel {

  public abstract CharSequence title();

  @ColorRes
  public abstract int titleTextColorRes();

  public abstract CharSequence byline();

  @ColorRes
  public abstract int bylineTextColorRes();

  public abstract Optional<Bitmap> icon();

  public abstract Optional<Bitmap> thumbnail();

  public abstract int titleMaxLines();

  public abstract Optional<Integer> backgroundTintColor();

  public abstract boolean progressVisible();

  public static Builder builder() {
    return new AutoValue_SubmissionContentLinkUiModel.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder title(CharSequence title);

    public abstract Builder titleTextColorRes(@ColorRes int colorRes);

    public abstract Builder byline(CharSequence byline);

    public abstract Builder bylineTextColorRes(@ColorRes int colorRes);

    public abstract Builder icon(Optional<Bitmap> icon);

    public abstract Builder thumbnail(Optional<Bitmap> thumbnail);

    public abstract Builder titleMaxLines(int maxLines);

    public abstract Builder backgroundTintColor(Optional<Integer> color);

    public abstract Builder progressVisible(boolean visible);

    public abstract SubmissionContentLinkUiModel build();
  }
}
