package me.saket.dank.ui.submission.adapter;

import android.graphics.drawable.Drawable;
import androidx.annotation.ColorRes;

import com.google.auto.value.AutoValue;

import me.saket.dank.data.SpannableWithTextEquality;
import me.saket.dank.urlparser.Link;
import me.saket.dank.utils.Optional;

@AutoValue
public abstract class SubmissionContentLinkUiModel {

  public abstract SpannableWithTextEquality title();

  @ColorRes
  public abstract int titleTextColorRes();

  public abstract SpannableWithTextEquality byline();

  @ColorRes
  public abstract int bylineTextColorRes();

  public abstract Optional<Drawable> icon();

  public abstract Optional<Integer> iconBackgroundRes();

  public abstract Optional<Drawable> thumbnail();

  public abstract int titleMaxLines();

  public abstract Optional<Integer> backgroundTintColor();

  public abstract boolean progressVisible();

  public abstract Link link();

  public static Builder builder() {
    return new AutoValue_SubmissionContentLinkUiModel.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    abstract Builder title(SpannableWithTextEquality title);

    public Builder title(CharSequence title) {
      return title(SpannableWithTextEquality.wrap(title));
    }

    public abstract Builder titleTextColorRes(@ColorRes int colorRes);

    abstract Builder byline(SpannableWithTextEquality byline);

    public Builder byline(CharSequence byline) {
      return byline(SpannableWithTextEquality.wrap(byline));
    }

    public abstract Builder bylineTextColorRes(@ColorRes int colorRes);

    public abstract Builder icon(Optional<Drawable> icon);

    public abstract Builder iconBackgroundRes(Optional<Integer> icon);

    public abstract Builder thumbnail(Optional<Drawable> thumbnail);

    public abstract Builder titleMaxLines(int maxLines);

    public abstract Builder backgroundTintColor(Optional<Integer> color);

    public abstract Builder progressVisible(boolean visible);

    public abstract Builder link(Link link);

    public abstract SubmissionContentLinkUiModel build();
  }
}
