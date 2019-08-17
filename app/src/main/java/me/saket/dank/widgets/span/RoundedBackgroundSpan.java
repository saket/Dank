package me.saket.dank.widgets.span;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import androidx.annotation.ColorInt;
import androidx.annotation.Px;
import android.text.style.LineHeightSpan;
import android.text.style.ReplacementSpan;

public class RoundedBackgroundSpan extends ReplacementSpan implements LineHeightSpan {

  private final @Px int cornerRadius;
  private final @Px int textSize;
  private final @Px int horizPadding;
  private final @Px int vertPadding;
  private final @ColorInt int backgroundColor;
  private final @ColorInt int textColor;

  private RectF rect = new RectF();
  private Rect textHeightRect = new Rect();

  public RoundedBackgroundSpan(
      @ColorInt int backgroundColor,
      @ColorInt int textColor,
      @Px int cornerRadius,
      @Px int textSize,
      @Px int horizPadding,
      @Px int vertPadding)
  {
    this.backgroundColor = backgroundColor;
    this.textColor = textColor;
    this.cornerRadius = cornerRadius;
    this.textSize = textSize;
    this.horizPadding = horizPadding;
    this.vertPadding = vertPadding;
  }

  @Override
  public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int lineTop, int lineBase, int lineBottom, Paint paint) {
    paint.setTextSize(textSize);
    paint.getTextBounds(text.toString(), 0, text.length(), textHeightRect);

    float textWidth = paint.measureText(text, start, end);
    float textHeight = textHeightRect.height();

    float lineHeight = lineBottom - lineTop;
    int bottomBaselineMid = lineBottom - lineBase;

    float bgRight = x + textWidth + cornerRadius + horizPadding;
    float bgTop = lineHeight / 2 - textHeight / 2 - vertPadding / 2;
    float bgBottom = lineHeight / 2 + textHeight / 2 + vertPadding / 2 + bottomBaselineMid / 2;
    rect.set(x, bgTop, bgRight, bgBottom);

    paint.setColor(backgroundColor);
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);

    paint.setColor(textColor);
    canvas.drawText(text, start, end, x + cornerRadius / 2 + horizPadding / 2, lineBase, paint);
  }

  @Override
  public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
    return Math.round(paint.measureText(text, start, end));
  }

  @Override
  public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int v, Paint.FontMetricsInt fm) {
//    if (end == ((Spanned) text).getSpanEnd(this)) {
//    }
  }
}
