package me.saket.dank.markdownhints.spans;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.annotation.Px;
import android.text.style.LineBackgroundSpan;

public class RoundedBackgroundColorSpan implements LineBackgroundSpan {

  private final @ColorInt int backgroundColor;
  private final @Px int radius;
  private final int startLineNumber;
  private final int endLineNumber;

  public RoundedBackgroundColorSpan(@ColorInt int color, @Px int radius, int startLineNumber, int endLineNumber) {
    this.backgroundColor = color;
    this.radius = radius;
    this.startLineNumber = startLineNumber;
    this.endLineNumber = endLineNumber;
  }

  @Override
  public void drawBackground(
      Canvas canvas,
      Paint paint,
      int left,
      int right,
      int top,
      int baseline,
      int bottom,
      CharSequence text,
      int start,
      int end,
      int lineNumber)
  {
    int originalColor = paint.getColor();
    paint.setColor(backgroundColor);

    if (lineNumber == startLineNumber) {
      // For select rounded corners, we'll draw another rect with reduced size
      // to cover the rounded rect.
      canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint);
      canvas.drawRect(left, top + radius, right, bottom, paint);

    } else if (lineNumber == endLineNumber) {
      canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint);
      canvas.drawRect(left, top, right, bottom - radius, paint);

    } else {
      canvas.drawRect(left, top, right, bottom, paint);
    }

    paint.setColor(originalColor);
  }

  @ColorInt
  public int backgroundColor() {
    return backgroundColor;
  }

  @Px
  public int radius() {
    return radius;
  }

  public int endLineNumber() {
    return endLineNumber;
  }

  public int startLineNumber() {
    return startLineNumber;
  }
}
