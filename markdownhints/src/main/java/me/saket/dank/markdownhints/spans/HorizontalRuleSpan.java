package me.saket.dank.markdownhints.spans;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.LineBackgroundSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.Px;

public class HorizontalRuleSpan implements LineBackgroundSpan {

  private final CharSequence text;
  private final @ColorInt int ruleColor;
  private final @Px int ruleStrokeWidth;
  private final Mode mode;

  private float leftOffset = -1;

  public enum Mode {
    HYPHENS {
      @Override
      public float getTopOffsetFactor() {
        return 0.07f;
      }
    },

    ASTERISKS {
      @Override
      public float getTopOffsetFactor() {
        return -0.11f;
      }
    },

    UNDERSCORES {
      @Override
      public float getTopOffsetFactor() {
        return 0.42f;
      }
    };

    /**
     * Used for centering the rule with the text.
     */
    public float getTopOffsetFactor() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * @param text Used for calculating the left offset to avoid drawing under the text.
   */
  public HorizontalRuleSpan(CharSequence text, @ColorInt int ruleColor, @Px int ruleStrokeWidth, Mode mode) {
    this.text = text;
    this.ruleColor = ruleColor;
    this.ruleStrokeWidth = ruleStrokeWidth;
    this.mode = mode;
  }

  @Override
  public void drawBackground(Canvas canvas, Paint paint, int left, int right, int top, int baseline, int bottom, CharSequence ignored,
      int start, int end, int lineNumber)
  {
    int originalPaintColor = paint.getColor();
    paint.setColor(ruleColor);
    paint.setStrokeWidth(ruleStrokeWidth);

    if (leftOffset == -1) {
      leftOffset = paint.measureText(text.toString());
    }

    int lineCenter = (int) ((top + bottom) / 2 + (paint.getTextSize() * mode.getTopOffsetFactor()));
    canvas.drawLine(left + leftOffset, lineCenter, right, lineCenter, paint);

    paint.setColor(originalPaintColor);
  }

  public CharSequence getText() {
    return text;
  }

  @ColorInt
  public int getRuleColor() {
    return ruleColor;
  }

  @Px
  public int getRuleStrokeWidth() {
    return ruleStrokeWidth;
  }

  public Mode getMode() {
    return mode;
  }
}
