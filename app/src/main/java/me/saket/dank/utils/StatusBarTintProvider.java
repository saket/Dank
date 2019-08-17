package me.saket.dank.utils;

import android.graphics.Bitmap;
import androidx.annotation.CheckResult;
import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.WorkerThread;
import androidx.palette.graphics.Palette;

import io.reactivex.Single;
import io.reactivex.functions.Function;
import me.saket.dank.data.StatusBarTint;

/**
 * Extracts colors from Bitmap and tweaks them for making them suitable for the status bar.
 */
public class StatusBarTintProvider {

  private static final float SCRIM_ADJUSTMENT = 0.075f;

  private final int statusBarHeight;
  private final int defaultStatusBarColor;
  private final int displayWidth;

  public StatusBarTintProvider(int defaultStatusBarColor, int statusBarHeight, int displayWidth) {
    this.statusBarHeight = statusBarHeight;
    this.defaultStatusBarColor = defaultStatusBarColor;
    this.displayWidth = displayWidth;
  }

  @CheckResult
  public Single<StatusBarTint> generateTint(Bitmap bitmap) {
    return generatePaletteFromBitmap(bitmap).map(generateTintFromPalette(bitmap));
  }

  @CheckResult
  private Single<Palette> generatePaletteFromBitmap(Bitmap bitmap) {
    return Single.fromCallable(() -> {
      int imageHeightToUse = (int) (statusBarHeight * ((float) bitmap.getWidth() / displayWidth));

      return Palette.from(bitmap)
          .maximumColorCount(3)
          .clearFilters() /* by default palette ignore certain hues (e.g. pure black/white) but we don't want this. */
          .setRegion(0, 0, bitmap.getWidth(), imageHeightToUse)
          .generate();
    });
  }

  private Function<Palette, StatusBarTint> generateTintFromPalette(Bitmap bitmap) {
    return palette -> {
      // Color the status bar. Set a complementary dark color on L,
      // light or dark color on M (with matching status bar icons).
      int statusBarColor = defaultStatusBarColor;
      Palette.Swatch topSwatch = palette.getDominantSwatch();
      TintColorUtils.Lightness lightness = topSwatch != null
          ? TintColorUtils.isDark(topSwatch)
          : TintColorUtils.Lightness.LIGHTNESS_UNKNOWN;

      boolean isDarkPalette = lightness == TintColorUtils.Lightness.LIGHTNESS_UNKNOWN
          ? TintColorUtils.isDark(bitmap, bitmap.getWidth() / 2, 0)
          : lightness == TintColorUtils.Lightness.IS_DARK;

      // TODO: Uncomment the condition after testing on Lollipop.
      if (topSwatch != null
        //&& (isDarkPalette || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
          ) {
        statusBarColor = TintColorUtils.scrimify(topSwatch.getRgb(), isDarkPalette, SCRIM_ADJUSTMENT);
      }

      //statusBarColor = TintColorUtils.scrimify(topSwatch.getRgb(), 0.075f);
      return StatusBarTint.create(statusBarColor, isDarkPalette);
    };
  }

  public static class TintColorUtils {
    public enum Lightness {
      IS_LIGHT, IS_DARK, LIGHTNESS_UNKNOWN
    }

    /**
     * Checks if the most populous color in the given palette is dark.
     * <p/>
     * Annoyingly we have to return this Lightness 'enum' rather than a boolean as palette isn't
     * guaranteed to find the most populous color.
     */
    public static Lightness isDark(Palette.Swatch swatch) {
      return isDark(swatch.getHsl()) ? Lightness.IS_DARK : Lightness.IS_LIGHT;
    }

    /**
     * Determines if a given bitmap is dark. This extracts a palette inline so should not be called
     * with a large image!! If palette fails then check the color of the specified pixel
     */
    @WorkerThread
    public static boolean isDark(Bitmap bitmap, int backupPixelX, int backupPixelY) {
      // First try palette with a small color quant size.
      Palette palette = Palette.from(bitmap).maximumColorCount(3).generate();
      if (palette.getSwatches().size() > 0) {
        //noinspection ConstantConditions
        return isDark(palette.getDominantSwatch()) == Lightness.IS_DARK;
      } else {
        // If palette failed, then check the color of the specified pixel.
        return isDark(bitmap.getPixel(backupPixelX, backupPixelY));
      }
    }

    /**
     * Check that the lightness value (0–1)
     */
    public static boolean isDark(float[] hsl) { // @Size(3)
      return hsl[2] < 0.5f;
    }

    /**
     * Convert to HSL & check that the lightness value
     */
    public static boolean isDark(@ColorInt int color) {
      float[] hsl = new float[3];
      androidx.core.graphics.ColorUtils.colorToHSL(color, hsl);
      return isDark(hsl);
    }

    /**
     * Calculate a variant of the color to make it more suitable for overlaying information. Light
     * colors will be lightened and dark colors will be darkened
     *
     * @param color               the color to adjust
     * @param isDark              whether {@code color} is light or dark
     * @param lightnessMultiplier the amount to modify the color e.g. 0.1f will alter it by 10%
     * @return the adjusted color
     */
    @ColorInt
    public static int scrimify(@ColorInt int color, boolean isDark, @FloatRange(from = 0f, to = 1f) float lightnessMultiplier) {
      float[] hsl = new float[3];
      androidx.core.graphics.ColorUtils.colorToHSL(color, hsl);

      if (!isDark) {
        lightnessMultiplier += 1f;
      } else {
        lightnessMultiplier = 1f - lightnessMultiplier;
      }

      hsl[2] = Math.max(0f, Math.min(1f, hsl[2] * lightnessMultiplier));
      return androidx.core.graphics.ColorUtils.HSLToColor(hsl);
    }

    @ColorInt
    public static int scrimify(@ColorInt int color, @FloatRange(from = 0f, to = 1f) float lightnessMultiplier) {
      return scrimify(color, isDark(color), lightnessMultiplier);
    }
  }
}
