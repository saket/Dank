package me.saket.dank.widgets;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import android.widget.TextView;

public class TextDrawable extends Drawable {

  private final String text;
  private final TextView textView;

  /**
   * Creates a drawable from the given text.
   */
  public TextDrawable(String text, TextView textView) {
    this.text = text;
    this.textView = textView;
    setBounds(0, 0, (int) textView.getPaint().measureText(this.text) + text.length(), (int) textView.getTextSize());
  }

  @Override
  public void draw(Canvas canvas) {
    Paint paint = textView.getPaint();
    paint.setColor(textView.getTextColors().getColorForState(textView.getDrawableState(), 0));
    int lineBaseline = textView.getLineBounds(0, null);
    canvas.drawText(text, 0, canvas.getClipBounds().top + lineBaseline, paint);
  }

  @Override
  public void setAlpha(int alpha) {/* Not supported */}

  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {/* Not supported */}

  @Override
  public int getOpacity() {
    return PixelFormat.OPAQUE;
  }
}
