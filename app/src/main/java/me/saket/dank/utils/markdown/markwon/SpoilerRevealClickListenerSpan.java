package me.saket.dank.utils.markdown.markwon;

import android.text.TextPaint;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

public class SpoilerRevealClickListenerSpan extends URLSpan {

  private final SpoilerLabelSpan labelSpan;
  private final SpoilerContentSpan contentSpan;

  public SpoilerRevealClickListenerSpan(SpoilerLabelSpan labelSpan, SpoilerContentSpan contentSpan) {
    super("/s");
    this.labelSpan = labelSpan;
    this.contentSpan = contentSpan;
  }

  @Override
  public void onClick(View widget) {
    int revealedTextColor = ((TextView) widget).getCurrentTextColor();
    labelSpan.setHidden(true);
    contentSpan.setRevealed(true, revealedTextColor);

    // TextView strangely does not update spans when invalidate() is called if
    // the TextView is selectable. Toggling enabled state seems to reset something
    // and update the spans.
    widget.setEnabled(false);
    widget.setEnabled(true);
  }

  @Override
  public void updateDrawState(TextPaint ds) {
    // no-op.
  }
}
