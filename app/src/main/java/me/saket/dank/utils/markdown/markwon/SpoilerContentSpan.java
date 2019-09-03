package me.saket.dank.utils.markdown.markwon;

import android.text.TextPaint;
import android.text.style.CharacterStyle;

import androidx.annotation.ColorInt;

public class SpoilerContentSpan extends CharacterStyle {

  private final @ColorInt int backgroundColor;
  private final String spoilerContent;

  private boolean isRevealed;
  private @ColorInt int revealedTextColor;

  public SpoilerContentSpan(@ColorInt int backgroundColor, String spoilerContent) {
    super();
    this.backgroundColor = backgroundColor;
    this.spoilerContent = spoilerContent;
  }

  public void setRevealed(boolean revealed, @ColorInt int revealedTextColor) {
    this.isRevealed = revealed;
    this.revealedTextColor = revealedTextColor;
  }

  @Override
  public void updateDrawState(TextPaint ds) {
    ds.bgColor = backgroundColor;
    ds.setColor(isRevealed ? revealedTextColor : backgroundColor);
  }
}
