package me.saket.dank.markdownhints;

import android.text.Editable;
import android.text.Spannable;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import me.saket.dank.markdownhints.spans.CustomQuoteSpan;
import me.saket.dank.markdownhints.spans.HorizontalRuleSpan;
import timber.log.Timber;

/**
 * TODO: Remove formatting from pasted content.
 * <p>
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
  }

  private final EditText editText;
  private final SpanPool spanPool;
  private final Parser parser;
  private final MarkdownNodeTreeVisitor markdownNodeTreeVisitor;
  private final MarkdownHintsSpanWriter markdownHintsSpanWriter;

  public static void enableLogging() {
    Timber.plant(new Timber.DebugTree());
  }

  public MarkdownHints(EditText editText, HighlightOptions highlightOptions, SpanPool spanPool) {
    this.editText = editText;
    this.spanPool = spanPool;
    this.markdownHintsSpanWriter = new MarkdownHintsSpanWriter();
    this.markdownNodeTreeVisitor = new MarkdownNodeTreeVisitor(spanPool, highlightOptions);

    parser = Parser.builder()
        .extensions(Collections.singletonList(StrikethroughExtension.create()))
        .build();
  }

  @Override
  public void afterTextChanged(Editable editable) {
    //Log.d("MH", "---------------------");
    //Log.i("MH", "text changed: " + editable);
    editText.removeTextChangedListener(this);

    removeStylingSpans(editable);
    Node markdownRootNode = parser.parse(editable.toString());
    markdownHintsSpanWriter.setText(editable);
    markdownNodeTreeVisitor.visit(markdownRootNode, markdownHintsSpanWriter);

    editText.addTextChangedListener(this);
  }

  private void removeStylingSpans(Spannable spannable) {
    Object[] spans = spannable.getSpans(0, spannable.length(), Object.class);
    for (Object span : spans) {
      if (SUPPORTED_MARKDOWN_SPANS.contains(span.getClass())) {
        //Log.i("MH", "removing span: " + span);
        spannable.removeSpan(span);
        spanPool.recycle(span);
      }
      //else {
      //  Log.i("MH", "ignoring: " + span.getClass().getName());
      //}
    }
  }
}
