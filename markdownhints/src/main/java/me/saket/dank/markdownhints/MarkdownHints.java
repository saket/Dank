package me.saket.dank.markdownhints;

import android.text.Editable;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.widget.EditText;

import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.sequence.SubSequence;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import me.saket.dank.markdownhints.spans.CustomQuoteSpan;
import me.saket.dank.markdownhints.spans.HorizontalRuleSpan;
import timber.log.Timber;

/**
 * Usage: EditText#addTextChangedListener(new MarkdownHints(EditText, HighlightOptions, SpanPool));
 */
public class MarkdownHints extends SimpleTextWatcher {

  public static final Set<Object> SUPPORTED_MARKDOWN_SPANS = new HashSet<>();

  static {
    SUPPORTED_MARKDOWN_SPANS.add(StyleSpan.class);
    SUPPORTED_MARKDOWN_SPANS.add(ForegroundColorSpan.class);
    SUPPORTED_MARKDOWN_SPANS.add(StrikethroughSpan.class);
    SUPPORTED_MARKDOWN_SPANS.add(TypefaceSpan.class);
    SUPPORTED_MARKDOWN_SPANS.add(RelativeSizeSpan.class);
    SUPPORTED_MARKDOWN_SPANS.add(SuperscriptSpan.class);
    SUPPORTED_MARKDOWN_SPANS.add(CustomQuoteSpan.class);
    SUPPORTED_MARKDOWN_SPANS.add(LeadingMarginSpan.Standard.class);
    SUPPORTED_MARKDOWN_SPANS.add(HorizontalRuleSpan.class);
    SUPPORTED_MARKDOWN_SPANS.add(BackgroundColorSpan.class);
  }

  private final EditText editText;
  private final MarkdownSpanPool spanPool;
  private final Parser parser;
  private final MarkdownNodeTreeVisitor markdownNodeTreeVisitor;
  private final MarkdownHintsSpanWriter markdownHintsSpanWriter;

  public static void enableLogging() {
    Timber.plant(new Timber.DebugTree());
  }

  public MarkdownHints(EditText editText, MarkdownHintOptions markdownHintOptions, MarkdownSpanPool spanPool) {
    this.editText = editText;
    this.spanPool = spanPool;
    this.markdownHintsSpanWriter = new MarkdownHintsSpanWriter();
    this.markdownNodeTreeVisitor = new MarkdownNodeTreeVisitor(spanPool, markdownHintOptions);

    parser = Parser.builder()
        .extensions(Collections.singletonList(StrikethroughExtension.create()))
        .build();
  }

  @Override
  public void afterTextChanged(Editable editable) {
    editText.removeTextChangedListener(this);

    // Remove all spans inserted in the previous text
    // change call or else we'll see stale styling.
    removeHintSpans(editable);

    Node markdownRootNode = parser.parse(SubSequence.of(editable));
    markdownHintsSpanWriter.setText(editable);
    markdownNodeTreeVisitor.visit(markdownRootNode, markdownHintsSpanWriter);

    editText.addTextChangedListener(this);
  }

  private void removeHintSpans(Spannable spannable) {
    Object[] spans = spannable.getSpans(0, spannable.length(), Object.class);
    for (Object span : spans) {
      if (SUPPORTED_MARKDOWN_SPANS.contains(span.getClass())) {
        spannable.removeSpan(span);
        spanPool.recycle(span);
      }
    }
  }
}
