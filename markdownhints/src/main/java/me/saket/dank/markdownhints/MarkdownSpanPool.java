package me.saket.dank.markdownhints;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import androidx.annotation.ColorInt;
import androidx.annotation.Px;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import me.saket.dank.markdownhints.spans.HeadingSpanWithLevel;
import me.saket.dank.markdownhints.spans.HorizontalRuleSpan;
import me.saket.dank.markdownhints.spans.IndentedCodeBlockSpan;
import me.saket.dank.markdownhints.spans.InlineCodeSpan;
import ru.noties.markwon.spans.BlockQuoteSpan;
import ru.noties.markwon.spans.SpannableTheme;

/**
 * For avoiding creation of new spans on every text change.
 */
@SuppressLint("UseSparseArrays")
public class MarkdownSpanPool {

  private final Stack<StyleSpan> italicsSpans = new Stack<>();
  private final Stack<StyleSpan> boldSpans = new Stack<>();
  private final Stack<StrikethroughSpan> strikethroughSpans = new Stack<>();
  private final Stack<TypefaceSpan> monospaceTypefaceSpans = new Stack<>();
  private final Map<Integer, ForegroundColorSpan> foregroundColorSpans = new HashMap<>();
  private final Stack<InlineCodeSpan> inlineCodeSpans = new Stack<>();
  private final Stack<IndentedCodeBlockSpan> indentedCodeSpans = new Stack<>();
  private final Map<Integer, HeadingSpanWithLevel> headingSpans = new HashMap<>();
  private final Stack<SuperscriptSpan> superscriptSpans = new Stack<>();
  private final Stack<BlockQuoteSpan> quoteSpans = new Stack<>();
  private final Map<Integer, LeadingMarginSpan.Standard> leadingMarginSpans = new HashMap<>();
  private final Map<String, HorizontalRuleSpan> horizontalRuleSpans = new HashMap<>();

  private final SpannableTheme spannableTheme;

  public MarkdownSpanPool(SpannableTheme spannableTheme) {
    this.spannableTheme = spannableTheme;
  }

  public StyleSpan italics() {
    return italicsSpans.empty() ? new StyleSpan(Typeface.ITALIC) : italicsSpans.pop();
  }

  public StyleSpan bold() {
    return boldSpans.empty()
        ? new StyleSpan(Typeface.BOLD)
        : boldSpans.pop();
  }

  public ForegroundColorSpan foregroundColor(@ColorInt int color) {
    return foregroundColorSpans.containsKey(color)
        ? foregroundColorSpans.remove(color)
        : new ForegroundColorSpan(color);
  }

  public InlineCodeSpan inlineCode() {
    return inlineCodeSpans.empty()
        ? new InlineCodeSpan(spannableTheme)
        : inlineCodeSpans.pop();
  }

  public IndentedCodeBlockSpan indentedCodeBlock() {
    return indentedCodeSpans.empty()
        ? new IndentedCodeBlockSpan(spannableTheme)
        : indentedCodeSpans.pop();
  }

  public StrikethroughSpan strikethrough() {
    return strikethroughSpans.empty()
        ? new StrikethroughSpan()
        : strikethroughSpans.pop();
  }

  public TypefaceSpan monospaceTypeface() {
    return monospaceTypefaceSpans.empty()
        ? new TypefaceSpan("monospace")
        : monospaceTypefaceSpans.pop();
  }

  public HeadingSpanWithLevel heading(int level) {
    return headingSpans.containsKey(level)
        ? headingSpans.remove(level)
        : new HeadingSpanWithLevel(spannableTheme, level);
  }

  public SuperscriptSpan superscript() {
    return superscriptSpans.empty()
        ? new SuperscriptSpan()
        : superscriptSpans.pop();
  }

  public BlockQuoteSpan quote() {
    return quoteSpans.empty()
        ? new BlockQuoteSpan(spannableTheme)
        : quoteSpans.pop();
  }

  public LeadingMarginSpan.Standard leadingMargin(int margin) {
    return leadingMarginSpans.containsKey(margin) ? leadingMarginSpans.remove(margin) : new LeadingMarginSpan.Standard(margin);
  }

  /**
   * @param text See {@link HorizontalRuleSpan#HorizontalRuleSpan(CharSequence, int, int, HorizontalRuleSpan.Mode)}.
   */
  public HorizontalRuleSpan horizontalRule(CharSequence text, @ColorInt int ruleColor, @Px int ruleStrokeWidth, HorizontalRuleSpan.Mode mode) {
    String key = text + "_" + ruleColor + "_" + ruleStrokeWidth + "_" + mode;
    return horizontalRuleSpans.containsKey(key)
        ? horizontalRuleSpans.remove(key)
        : new HorizontalRuleSpan(text, ruleColor, ruleStrokeWidth, mode);
  }

  public void recycle(Object span) {
    if (span instanceof StyleSpan) {
      recycle(((StyleSpan) span));

    } else if (span instanceof ForegroundColorSpan) {
      recycle(((ForegroundColorSpan) span));

    } else if (span instanceof StrikethroughSpan) {
      recycle(((StrikethroughSpan) span));

    } else if (span instanceof TypefaceSpan) {
      recycle(((TypefaceSpan) span));

    } else if (span instanceof HeadingSpanWithLevel) {
      recycle(((HeadingSpanWithLevel) span));

    } else if (span instanceof SuperscriptSpan) {
      recycle(((SuperscriptSpan) span));

    } else if (span instanceof BlockQuoteSpan) {
      recycle(((BlockQuoteSpan) span));

    } else if (span instanceof LeadingMarginSpan.Standard) {
      recycle(((LeadingMarginSpan.Standard) span));

    } else if (span instanceof HorizontalRuleSpan) {
      recycle(((HorizontalRuleSpan) span));

    } else if (span instanceof IndentedCodeBlockSpan) {
      recycle(((IndentedCodeBlockSpan) span));

    } else if (span instanceof InlineCodeSpan) {
      recycle(((InlineCodeSpan) span));

    } else {
      throw new UnsupportedOperationException("Unknown span: " + span.getClass().getSimpleName());
    }
  }

  public void recycle(StyleSpan span) {
    if ((span).getStyle() == Typeface.ITALIC) {
      italicsSpans.push(span);

    } else if ((span).getStyle() == Typeface.BOLD) {
      boldSpans.add(span);

    } else {
      throw new UnsupportedOperationException("Only italics and bold spans supported.");
    }
  }

  public void recycle(ForegroundColorSpan span) {
    int key = span.getForegroundColor();
    foregroundColorSpans.put(key, span);
  }

  public void recycle(InlineCodeSpan span) {
    inlineCodeSpans.push(span);
  }

  public void recycle(IndentedCodeBlockSpan span) {
    indentedCodeSpans.push(span);
  }

  public void recycle(StrikethroughSpan span) {
    strikethroughSpans.push(span);
  }

  public void recycle(TypefaceSpan span) {
    if (!span.getFamily().equals("monospace")) {
      throw new UnsupportedOperationException("Only monospace typeface spans exist in this pool.");
    }
    monospaceTypefaceSpans.push(span);
  }

  public void recycle(HeadingSpanWithLevel span) {
    headingSpans.put(span.level(), span);
  }

  public void recycle(SuperscriptSpan span) {
    superscriptSpans.push(span);
  }

  public void recycle(BlockQuoteSpan span) {
    quoteSpans.push(span);
  }

  public void recycle(LeadingMarginSpan.Standard span) {
    int key = span.getLeadingMargin(true /* irrelevant */);
    leadingMarginSpans.put(key, span);
  }

  public void recycle(HorizontalRuleSpan span) {
    String key = span.getText() + "_" + span.getRuleColor() + "_" + span.getRuleStrokeWidth() + "_" + span.getMode();
    horizontalRuleSpans.put(key, span);
  }
}
