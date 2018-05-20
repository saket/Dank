package me.saket.dank.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ProgressBar;

/**
 * A ProgressBar that uses height animation for hiding/showing + animates changes in determinate progress
 * (because {@link ProgressBar#setProgress(int, boolean)} is API 24+ only).
 */
public class SubmissionAnimatedProgressBar extends AnimatedProgressBar {

  private boolean syncScrollEnabled;

  public SubmissionAnimatedProgressBar(Context context, AttributeSet attrs) {
    super(context, attrs);
    super.setVisibility(VISIBLE);
  }

  /**
   * Tracks <var>sheet</var>'s top offset and keeps this View always on top of it.
   * Since {@link ScrollingRecyclerViewSheet} uses translationY changes to scroll, this
   */
  public void syncPositionWithSheet(ScrollingRecyclerViewSheet sheet) {
    sheet.addOnSheetScrollChangeListener((newTranslationY) -> {
      if (syncScrollEnabled && getTranslationY() != newTranslationY) {
        setTranslationY(newTranslationY);
      }
    });
  }

  /**
   * When enabled, this View keeps its top in sync with the sheet passed in {@link
   * #syncPositionWithSheet(ScrollingRecyclerViewSheet)} by scrolling along with it.
   */
  public void setSyncScrollEnabled(boolean enabled) {
    syncScrollEnabled = enabled;
  }

  public void showForReal() {
    // This will exist until I'm sure that there are no
    // remaining usages of show() and hide() left.
    super.show();
  }

  public void hideForReal() {
    super.hide();
  }

  /** @deprecated Use {@link #showForReal()} instead. */
  @Override
  public void show() {
    throw new UnsupportedOperationException();
  }

  /** @deprecated Use {@link #showForReal()} instead. */
  @Override
  public void hide() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setVisibility(int visibility) {
    super.setVisibility(visibility);
    new Exception().printStackTrace();
  }
}
