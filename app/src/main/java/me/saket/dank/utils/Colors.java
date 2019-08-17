package me.saket.dank.utils;

import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;

/**
 * Utility methods for manipulating colors.
 */
public class Colors {

  /**
   * Adds transparency to a color.
   *
   * @param color        Color in the format 0xFFFFFFFF.
   * @param alphaPercent transparency level
   * @return color with the alpha value set
   * <p>
   * Usage: setColorAlpha(0xFFFFFFFF, 0.5f);
   */
  public static int applyAlpha(@ColorInt int color, @FloatRange(from = 0, to = 1) float alphaPercent) {
    int alpha = Math.round(Color.alpha(color) * alphaPercent);
    return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
  }

  public static boolean isLight(int color) {
    double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
    return darkness < 0.45;
  }

  public static int mix(int color1, int white2) {
    int red = (Color.red(white2) + Color.red(color1)) / 2;
    int green = (Color.green(white2) + Color.green(color1)) / 2;
    int blue = (Color.blue(white2) + Color.blue(color1)) / 2;
    return Color.rgb(red, green, blue);
  }

  /**
   * Converts a color integer back to its hex form. Does not support alpha channel.
   * Example: 0xFFFFFFFF -> #FFFFFF
   */
  public static String colorIntToHex(@ColorInt int colorInt) {
    return "#" + Integer.toHexString(colorInt).toUpperCase().substring(2);
  }
}
