package me.saket.dank.data;

import android.text.SpannableString;

import me.saket.dank.ui.submission.adapter.SubmissionCommentsHeader;
import me.saket.dank.utils.lifecycle.LifecycleStreams;

/**
 * Spans don't implement value/content equality and the framework depends on reference equality instead.
 * This messes up logic in parts like DiffUtils where the same text is considered changed on every data
 * update. As a workaround, this class will only compare the text and no spans. If you have spans,
 * compare them in some other way. For example, {@link SubmissionCommentsHeader} includes dedicated
 * fields that represent changeable spans so that when equals() is called, it returns false when those
 * spans change.
 */
public class SpannableWithTextEquality extends SpannableString {

  private static final Object EMPTY_SPANS_VALUE = LifecycleStreams.NOTHING;

  private final Object spansValue;

  public static SpannableWithTextEquality wrap(CharSequence source) {
    return new SpannableWithTextEquality(source, EMPTY_SPANS_VALUE);
  }

  /**
   * @param spansValue In case source.equals() isn't enough. See equals().
   */
  public static SpannableWithTextEquality wrap(CharSequence source, Object spansValue) {
    return new SpannableWithTextEquality(source, spansValue);
  }

  // This is not what you're looking for.
  @Deprecated
  public static SpannableWithTextEquality valueOf(CharSequence ignore) {
    throw new AssertionError();
  }

  private SpannableWithTextEquality(CharSequence source, Object spansValue) {
    super(source);
    this.spansValue = spansValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SpannableWithTextEquality)) {
      return false;
    }

    SpannableWithTextEquality that = (SpannableWithTextEquality) o;
    return toString().equals(that.toString()) && spansValue.equals(that.spansValue);
  }

  @Override
  public int hashCode() {
    return toString().hashCode() + spansValue.hashCode();
  }
}
