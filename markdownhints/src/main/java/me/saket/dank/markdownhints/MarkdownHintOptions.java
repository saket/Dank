package me.saket.dank.markdownhints;

import androidx.annotation.ColorInt;
import androidx.annotation.Px;

import com.google.auto.value.AutoValue;

/**
 * Colors used for highlighting Markdown syntax and their styling.
 */
@AutoValue
public abstract class MarkdownHintOptions {

  @ColorInt
  public abstract int syntaxColor();

  @ColorInt
  public abstract int blockQuoteIndentationRuleColor();

  @ColorInt
  public abstract int blockQuoteTextColor();

  /**
   * Gap before a block of ordered/unordered list.
   */
  @Px
  public abstract int listBlockIndentationMargin();

  /**
   * Width of a block-quote's vertical line/stripe/rule.
   */
  @Px
  public abstract int blockQuoteVerticalRuleStrokeWidth();

  @ColorInt
  public abstract int linkUrlColor();

  @ColorInt
  public abstract int linkTextColor();

  @ColorInt
  public abstract int spoilerSyntaxHintColor();

  @ColorInt
  public abstract int spoilerHiddenContentOverlayColor();

  @ColorInt
  public abstract int horizontalRuleColor();

  @Px
  public abstract int horizontalRuleStrokeWidth();

  @ColorInt
  public abstract int inlineCodeBackgroundColor();

  @ColorInt
  public abstract int tableBorderColor();

  public static Builder builder() {
    return new AutoValue_MarkdownHintOptions.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder syntaxColor(@ColorInt int color);

    public abstract Builder blockQuoteIndentationRuleColor(@ColorInt int color);

    public abstract Builder blockQuoteTextColor(@ColorInt int color);

    /** See {@link #listBlockIndentationMargin()}. */
    public abstract Builder listBlockIndentationMargin(@Px int margin);

    /** See {@link #blockQuoteVerticalRuleStrokeWidth(). */
    public abstract Builder blockQuoteVerticalRuleStrokeWidth(@Px int width);

    public abstract Builder linkUrlColor(@ColorInt int color);

    public abstract Builder linkTextColor(@ColorInt int color);

    public abstract Builder spoilerSyntaxHintColor(@ColorInt int color);

    public abstract Builder spoilerHiddenContentOverlayColor(@ColorInt int color);

    public abstract Builder horizontalRuleColor(@ColorInt int color);

    public abstract Builder horizontalRuleStrokeWidth(@Px int width);

    public abstract Builder inlineCodeBackgroundColor(@ColorInt int color);

    public abstract Builder tableBorderColor(@ColorInt int color);

    public abstract MarkdownHintOptions build();
  }
}
