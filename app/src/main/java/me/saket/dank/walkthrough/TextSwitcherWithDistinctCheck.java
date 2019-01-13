package me.saket.dank.walkthrough;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextSwitcher;
import android.widget.TextView;

public class TextSwitcherWithDistinctCheck extends TextSwitcher {

  private CharSequence prevText;

  @SuppressLint("SetTextI18n")
  public TextSwitcherWithDistinctCheck(Context context, AttributeSet attrs) {
    super(context, attrs);

    if (isInEditMode()) {
      TextView textView = new TextView(context);
      textView.setText("Preview");
      addView(textView);
    }
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
