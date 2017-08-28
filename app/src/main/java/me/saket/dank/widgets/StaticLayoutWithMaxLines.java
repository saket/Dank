package me.saket.dank.widgets;

import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils.TruncateAt;

import java.lang.reflect.Constructor;

import timber.log.Timber;

public class StaticLayoutWithMaxLines {

  private static final String TEXT_DIR_CLASS = "android.text.TextDirectionHeuristic";
  private static final String TEXT_DIRS_CLASS = "android.text.TextDirectionHeuristics";
  private static final String TEXT_DIR_FIRSTSTRONG_LTR = "FIRSTSTRONG_LTR";

  private static boolean sInitialized;
  private static Constructor<StaticLayout> sConstructor;
  private static Object[] sConstructorArgs;
  private static Object sTextDirection;

  public static synchronized void ensureInitialized() {
    if (sInitialized) {
      return;
    }

    try {
      final Class<?> textDirClass;
      textDirClass = TextDirectionHeuristic.class;
      sTextDirection = TextDirectionHeuristics.FIRSTSTRONG_LTR;

      final Class<?>[] signature = new Class[] {
          CharSequence.class,
          int.class,
          int.class,
          TextPaint.class,
          int.class,
          Alignment.class,
          textDirClass,
          float.class,
          float.class,
          boolean.class,
          TruncateAt.class,
          int.class,
          int.class
      };

      // Make the StaticLayout constructor with max lines public
      sConstructor = StaticLayout.class.getDeclaredConstructor(signature);
      sConstructor.setAccessible(true);
      sConstructorArgs = new Object[signature.length];
    } catch (NoSuchMethodException e) {
      Timber.e(e, "StaticLayout constructor with max lines not found.");
    } finally {
      sInitialized = true;
    }
  }

  public static synchronized StaticLayout create(CharSequence source, TextPaint paint, int width, Alignment align, float spacingMult,
      float spacingAdd, boolean includePad, int maxLines)
  {
    int bufferEnd = source.length();
    return create(source, 0, bufferEnd, paint, width, align, spacingMult, spacingAdd, includePad, null, 0, maxLines);
  }

  private static synchronized StaticLayout create(CharSequence source, int bufstart, int bufend, TextPaint paint, int outerWidth, Alignment align,
      float spacingMult, float spacingAdd, boolean includePad, TruncateAt ellipsize, int ellipsisWidth, int maxLines)
  {
    ensureInitialized();

    try {
      sConstructorArgs[0] = source;
      sConstructorArgs[1] = bufstart;
      sConstructorArgs[2] = bufend;
      sConstructorArgs[3] = paint;
      sConstructorArgs[4] = outerWidth;
      sConstructorArgs[5] = align;
      sConstructorArgs[6] = sTextDirection;
      sConstructorArgs[7] = spacingMult;
      sConstructorArgs[8] = spacingAdd;
      sConstructorArgs[9] = includePad;
      sConstructorArgs[10] = ellipsize;
      sConstructorArgs[11] = ellipsisWidth;
      sConstructorArgs[12] = maxLines;

      return sConstructor.newInstance(sConstructorArgs);
    } catch (Exception e) {
      throw new IllegalStateException("Error creating StaticLayout with max lines: " + e);
    }
  }
}
