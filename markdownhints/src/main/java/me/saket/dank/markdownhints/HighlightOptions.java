package me.saket.dank.markdownhints;

import android.support.annotation.ColorInt;
import android.support.annotation.Px;
import com.google.auto.value.AutoValue;

/**
 * Colors used for highlighting Markdown syntax and their styling.
 */
@AutoValue
public abstract class HighlightOptions {

  @ColorInt
  public abstract int syntaxColor();

  @ColorInt
  public abstract int blockQuoteIndentationRuleColor();

  @ColorInt
  public abstract int blockQuoteTextColor();

  /**
   * Gap before a block of indented text e.g., block-quote, ordered/unordered list, etc.
   */
  @Px
  public abstract int textBlockIndentationMargin();

  /**
   * Width of a block-quote's vertical line/stripe/rule.
   */
  @Px
  public abstract int blockQuoteVerticalRuleStrokeWidth();

  @ColorInt
  public abstract int linkUrlColor();

  @ColorInt
  public abstract int horizontalRuleColor();

  @Px
  public abstract int horizontalRuleStrokeWidth();

  public static Builder builder() {
    return new AutoValue_HighlightOptions.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder syntaxColor(@ColorInt int color);

    public abstract Builder blockQuoteIndentationRuleColor(@ColorInt int color);

    public abstract Builder blockQuoteTextColor(@ColorInt int color);

    public abstract Builder textBlockIndentationMargin(@Px int margin);

    public abstract Builder blockQuoteVerticalRuleStrokeWidth(@Px int width);

    public abstract Builder linkUrlColor(@ColorInt int color);

    public abstract Builder horizontalRuleColor(@ColorInt int color);

    public abstract Builder horizontalRuleStrokeWidth(@Px int width);

    public abstract HighlightOptions build();
  }
}