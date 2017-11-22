package me.saket.dank.markdownhints;

import android.text.Editable;
import android.text.Spanned;

/**
 * TODO: Doc.
 */
public class MarkdownHintsSpanWriter {

  private Editable editable;

  public void setText(Editable editable) {
    this.editable = editable;
  }

  /** Starts {@code span} at the current position in the builder. */
  public MarkdownHintsSpanWriter pushSpan(Object span, int start, int end) {
    if (!MarkdownHints.SUPPORTED_MARKDOWN_SPANS.contains(span.getClass())) {
      throw new IllegalArgumentException("Span not supported: " + span.getClass());
    }
    editable.setSpan(span, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    return this;
  }
}
