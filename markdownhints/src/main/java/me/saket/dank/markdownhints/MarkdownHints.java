package me.saket.dank.markdownhints;

import android.text.Editable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.widget.EditText;

import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.sequence.SubSequence;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import me.saket.dank.markdownhints.spans.HeadingSpanWithLevel;
import me.saket.dank.markdownhints.spans.HorizontalRuleSpan;
import me.saket.dank.markdownhints.spans.IndentedCodeBlockSpan;
import me.saket.dank.markdownhints.spans.InlineCodeSpan;
import ru.noties.markwon.spans.BlockQuoteSpan;
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
    SUPPORTED_MARKDOWN_SPANS.add(HeadingSpanWithLevel.class);
    SUPPORTED_MARKDOWN_SPANS.add(SuperscriptSpan.class);
    SUPPORTED_MARKDOWN_SPANS.add(BlockQuoteSpan.class);
    SUPPORTED_MARKDOWN_SPANS.add(LeadingMarginSpan.Standard.class);
    SUPPORTED_MARKDOWN_SPANS.add(HorizontalRuleSpan.class);
    SUPPORTED_MARKDOWN_SPANS.add(InlineCodeSpan.class);
    SUPPORTED_MARKDOWN_SPANS.add(IndentedCodeBlockSpan.class);
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

    // We'll see stale styling if previous spans aren't removed.
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
