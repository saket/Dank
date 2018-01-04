package me.saket.dank.data;

import android.text.SpannableString;

/**
 * Spans don't implement value/content equality and the framework depends on reference equality instead.
 * This messes up logic in parts like DiffUtils where the same text is considered changed on every data update.
 */
public class SpannableWithValueEquality extends SpannableString {

  public static SpannableWithValueEquality wrap(CharSequence source) {
    return new SpannableWithValueEquality(source);
  }

  // This is not what you're looking for.
  @Deprecated
  public static SpannableWithValueEquality valueOf(CharSequence ignore) {
    throw new AssertionError();
  }

  private SpannableWithValueEquality(CharSequence source) {
    super(source);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SpannableWithValueEquality)) {
      return false;
    }

    SpannableWithValueEquality that = (SpannableWithValueEquality) o;
    return toString().equals(that.toString());
  }
}
