package me.saket.dank.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.appcompat.widget.AppCompatTextView;
import android.util.AttributeSet;

/**
 * A TextView which tints its compound drawable with the same color as its text.
 */
public class TintableCompoundDrawableTextView extends AppCompatTextView {

  public TintableCompoundDrawableTextView(Context context) {
    super(context);
  }

  public TintableCompoundDrawableTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public TintableCompoundDrawableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    applyColorTintToCompoundDrawables(getCurrentTextColor());
  }

  @Override
  public void setCompoundDrawablesRelativeWithIntrinsicBounds(Drawable start, Drawable top, Drawable end, Drawable bottom) {
    super.setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, bottom);
    applyColorTintToCompoundDrawables(getCurrentTextColor());
  }

  @Override
  public void setTextColor(int color) {
    super.setTextColor(color);
    applyColorTintToCompoundDrawables(color);
  }

  /**
   * Applies color tint to all compound drawables set on Views extending TextView (eg., Button(s)).
   */
  public void applyColorTintToCompoundDrawables(@ColorInt int tintColor) {
    Drawable[] drawables;
    drawables = getCompoundDrawablesRelative();

    for (int i = 0; i < drawables.length; i++) {
      if (drawables[i] != null) {
        // Wrap the drawable so that future tinting calls work on pre-v21 devices. Always use the returned drawable.
        drawables[i] = drawables[i].mutate();
        drawables[i].setTint(tintColor);
      }
    }

    super.setCompoundDrawablesRelativeWithIntrinsicBounds(drawables[0], drawables[1], drawables[2], drawables[3]);
  }
}
