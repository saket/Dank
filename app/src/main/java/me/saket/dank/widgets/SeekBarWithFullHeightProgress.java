package me.saket.dank.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;

import androidx.appcompat.widget.AppCompatSeekBar;

/**
 * {@link SeekBar} strangely has a maximum height for progress drawable.
 */
public class SeekBarWithFullHeightProgress extends AppCompatSeekBar {

  public SeekBarWithFullHeightProgress(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    int left = getPaddingLeft();
    int top = getPaddingTop();
    getProgressDrawable().setBounds(left, top, w - left - getPaddingRight(), h - top - getPaddingBottom());
  }
}
