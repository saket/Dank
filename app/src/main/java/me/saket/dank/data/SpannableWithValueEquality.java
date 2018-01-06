package me.saket.dank.data;

import android.text.SpannableString;

import me.saket.dank.ui.submission.adapter.SubmissionCommentsHeader;

/**
 * Spans don't implement value/content equality and the framework depends on reference equality instead.
 * This messes up logic in parts like DiffUtils where the same text is considered changed on every data
 * update. As a workaround, this class will only compare the text and no spans. If you have spans,
 * compare them in some other way. For example, {@link SubmissionCommentsHeader} includes dedicated
 * fields that represent changeable spans so that when equals() is called, it returns false when those
 * spans change.
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
