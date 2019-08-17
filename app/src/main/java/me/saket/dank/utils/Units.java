package me.saket.dank.utils;

import android.content.Context;
import androidx.annotation.Px;
import android.util.TypedValue;

public class Units {

  @Px
  public static int dpToPx(float dpValue, Context context) {
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.getResources().getDisplayMetrics());
  }
}
