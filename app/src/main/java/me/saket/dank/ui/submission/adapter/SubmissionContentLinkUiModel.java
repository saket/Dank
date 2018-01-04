package me.saket.dank.ui.submission.adapter;

import android.graphics.Bitmap;
import android.support.annotation.ColorRes;

import com.google.auto.value.AutoValue;

import me.saket.dank.data.SpannableWithValueEquality;
import me.saket.dank.utils.Optional;

@AutoValue
public abstract class SubmissionContentLinkUiModel {

  public abstract SpannableWithValueEquality title();

  @ColorRes
  public abstract int titleTextColorRes();

  public abstract SpannableWithValueEquality byline();

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
    abstract Builder title(SpannableWithValueEquality title);

    public Builder title(CharSequence title) {
      return title(SpannableWithValueEquality.wrap(title));
    }

    public abstract Builder titleTextColorRes(@ColorRes int colorRes);

    abstract Builder byline(SpannableWithValueEquality byline);

    public Builder byline(CharSequence byline) {
      return byline(SpannableWithValueEquality.wrap(byline));
    }

    public abstract Builder bylineTextColorRes(@ColorRes int colorRes);

    public abstract Builder icon(Optional<Bitmap> icon);

    public abstract Builder thumbnail(Optional<Bitmap> thumbnail);

    public abstract Builder titleMaxLines(int maxLines);

    public abstract Builder backgroundTintColor(Optional<Integer> color);

    public abstract Builder progressVisible(boolean visible);

    public abstract SubmissionContentLinkUiModel build();
  }
}
