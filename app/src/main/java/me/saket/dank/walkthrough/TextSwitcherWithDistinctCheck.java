package me.saket.dank.walkthrough;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextSwitcher;

public class TextSwitcherWithDistinctCheck extends TextSwitcher {

  private CharSequence prevText;

  public TextSwitcherWithDistinctCheck(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void setText(CharSequence text) {
    if (text.equals(prevText)) {
      return;
    }
    prevText = text;
    super.setText(text);
  }
}
