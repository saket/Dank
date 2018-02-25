package me.saket.dank.widgets.span;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.LineBackgroundSpan;

/**
 * {@link LineBackgroundSpan} is used for drawing spans that cover the entire line.
 */
public class HorizontalRuleSpan implements LineBackgroundSpan {

  private final int ruleColor;          // Color of line
  private final float line;             // Line size

  public HorizontalRuleSpan(int ruleColor, float lineHeight) {
    this.ruleColor = ruleColor;
    this.line = lineHeight;
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
      int lnum)
  {
    int originalPaintColor = paint.getColor();
    float y = (float) (top + (bottom - top) / 2) - (line / 2);
    RectF lineRect = new RectF(left, y, (right - left), y + line);
    paint.setColor(ruleColor);
    canvas.drawRect(lineRect, paint);
    paint.setColor(originalPaintColor);
  }
}
