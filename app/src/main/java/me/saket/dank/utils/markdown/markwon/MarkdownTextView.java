package me.saket.dank.utils.markdown.markwon;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import ru.noties.markwon.Markwon;

public class MarkdownTextView extends AppCompatTextView {

  public MarkdownTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void setText(CharSequence text, BufferType type) {
    Markwon.unscheduleTableRows(this);
    super.setText(text, type);
    Markwon.scheduleTableRows(this);
  }
}
