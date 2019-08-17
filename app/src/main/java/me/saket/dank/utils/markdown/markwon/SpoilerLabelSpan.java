package me.saket.dank.utils.markdown.markwon;

import androidx.annotation.ColorInt;
import android.text.TextPaint;
import android.text.style.CharacterStyle;

public class SpoilerLabelSpan extends CharacterStyle {

  private final @ColorInt int backgroundColor;

  private boolean isHidden;

  public SpoilerLabelSpan(@ColorInt int backgroundColor) {
    super();
    this.backgroundColor = backgroundColor;
  }

  public void setHidden(boolean hidden) {
    this.isHidden = hidden;
  }

  @Override
  public void updateDrawState(TextPaint ds) {
    if (isHidden) {
      ds.setColor(backgroundColor);
    }
    ds.bgColor = backgroundColor;
  }
}
