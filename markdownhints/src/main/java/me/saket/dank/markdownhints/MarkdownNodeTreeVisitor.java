package me.saket.dank.markdownhints;

import android.support.annotation.ColorInt;
import android.support.annotation.Px;

import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.DelimitedNode;
import com.vladsch.flexmark.ast.Document;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.ListBlock;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ast.ThematicBreak;
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.SubSequence;

import me.saket.dank.markdownhints.spans.CustomQuoteSpan;
import me.saket.dank.markdownhints.spans.HorizontalRuleSpan;
import me.saket.dank.markdownhints.spans.RoundedBackgroundColorSpan;
import timber.log.Timber;

/**
 * To support:
 * - Superscript
 */
public class MarkdownNodeTreeVisitor {

  private static final float[] HEADING_SIZES = {
      1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1f,
  };
  private static final BasedSequence FOUR_ASTERISKS_HORIZONTAL_RULE = SubSequence.of("****");

  private final MarkdownSpanPool spanPool;
  private final MarkdownHintOptions options;
  private final @ColorInt int syntaxColor;
  private final @ColorInt int blockQuoteIndentationRuleColor;
  private final @ColorInt int blockQuoteTextColor;
  private final @ColorInt int linkUrlColor;
  private final @ColorInt int horizontalRuleColor;
  private final @Px int horizontalRuleStrokeWidth;
  private final @Px int blockQuoteVerticalRuleStrokeWidth;
  private final @Px int textBlockIndentationMargin;
  private MarkdownHintsSpanWriter writer;

  public MarkdownNodeTreeVisitor(MarkdownSpanPool spanPool, MarkdownHintOptions options) {
    this.spanPool = spanPool;
    this.options = options;
    syntaxColor = options.syntaxColor();
    blockQuoteIndentationRuleColor = options.blockQuoteIndentationRuleColor();
    blockQuoteTextColor = options.blockQuoteTextColor();
    textBlockIndentationMargin = options.textBlockIndentationMargin();
    blockQuoteVerticalRuleStrokeWidth = options.blockQuoteVerticalRuleStrokeWidth();
    linkUrlColor = options.linkUrlColor();
    horizontalRuleColor = options.horizontalRuleColor();
    horizontalRuleStrokeWidth = options.horizontalRuleStrokeWidth();
  }

  public void visit(Node markdownRootNode, MarkdownHintsSpanWriter hintsWriter) {
    this.writer = hintsWriter;
    visitChildren(markdownRootNode);
  }

  /**
   * Visit the child nodes.
   *
   * @param parent the parent node whose children should be visited
   */
  protected void visitChildren(Node parent) {
    Node node = parent.getFirstChild();
    while (node != null) {
      // A subclass of this visitor might modify the node, resulting in getNext returning a different node or no
      // node after visiting it. So get the next node before visiting.
      Node next = node.getNext();

      if (node instanceof Emphasis) {
        highlightItalics(((Emphasis) node));

      } else if (node instanceof StrongEmphasis) {
        highlightBold((StrongEmphasis) node);

      } else if (node instanceof Strikethrough) {
        highlightStrikeThrough((Strikethrough) node);

      } else if (node instanceof Heading) {
        // Setext styles aren't supported. Setext-style headers are "underlined" using equal signs
        // (for first-level headers) and dashes (for second-level headers). For example:
        // This is an H1
        // =============
        //
        // This is an H2
        // -------------
        if (((Heading) node).isAtxHeading()) {
          highlightHeading((Heading) node);

        } else {
          // Reddit allows thematic breaks without a leading new line. So we'll manually support this.
          highlightThematicBreakWithoutLeadingNewLine(((Heading) node));
        }

      } else if (node instanceof Link) {
        highlightLink(((Link) node));

      } else if (node instanceof Code) {
        highlightCode((Code) node);

      } else if (node instanceof IndentedCodeBlock) {
        highlightIndentedCodeBlock(((IndentedCodeBlock) node));

        // TODO.
        //} else if (node instanceof Superscript) {
        //  visit(((Superscript) node));

      } else if (node instanceof BlockQuote) {
        highlightBlockQuote(((BlockQuote) node));

      } else if (node instanceof ListBlock) {
        highlightListBlock(((ListBlock) node));

      } else if (node instanceof ListItem) {
        highlightListItem(((ListItem) node));

      } else if (node instanceof ThematicBreak) {
        // a.k.a. horizontal rule.
        highlightThematicBreak(((ThematicBreak) node));

      } else if (node instanceof Document || node instanceof Text || node instanceof Paragraph || node instanceof SoftLineBreak) {
        // Ignored.

      } else {
        Timber.w("Unknown node: %s", node);
      }
      visitChildren(node);

      node = next;
    }
  }

  public <T extends Node & DelimitedNode> void highlightMarkdownSyntax(T delimitedNode) {
    if (delimitedNode.getOpeningMarker().length() > 0) {
      writer.pushSpan(
          spanPool.foregroundColor(syntaxColor),
          delimitedNode.getStartOffset(),
          delimitedNode.getStartOffset() + delimitedNode.getOpeningMarker().length()
      );
    }

    if (delimitedNode.getClosingMarker().length() > 0) {
      writer.pushSpan(
          spanPool.foregroundColor(syntaxColor),
          delimitedNode.getEndOffset() - delimitedNode.getClosingMarker().length(),
          delimitedNode.getEndOffset()
      );
    }
  }

  public void highlightItalics(Emphasis emphasis) {
    writer.pushSpan(spanPool.italics(), emphasis.getStartOffset(), emphasis.getEndOffset());
    highlightMarkdownSyntax(emphasis);
  }

  public void highlightBold(StrongEmphasis strongEmphasis) {
    writer.pushSpan(spanPool.bold(), strongEmphasis.getStartOffset(), strongEmphasis.getEndOffset());
    highlightMarkdownSyntax(strongEmphasis);
  }

  public void highlightCode(Code code) {
    writer.pushSpan(spanPool.backgroundColor(options.indentedCodeBlockBackgroundColor()), code.getStartOffset(), code.getEndOffset());
    writer.pushSpan(spanPool.monospaceTypeface(), code.getStartOffset(), code.getEndOffset());
    highlightMarkdownSyntax(code);
  }

  public void highlightIndentedCodeBlock(IndentedCodeBlock indentedCodeBlock) {
    // LineBackgroundSpan needs to start at the starting of the line.
    int lineStartOffset = indentedCodeBlock.getStartOffset() - 4;
    int startLineNumber = indentedCodeBlock.getLineNumber();
    int endLineNumber = startLineNumber + 1 + indentedCodeBlock.getLineCount();
    RoundedBackgroundColorSpan span = spanPool.roundedBackgroundColor(
        options.indentedCodeBlockBackgroundColor(),
        options.indentedCodeBlockBackgroundRadius(),
        startLineNumber,
        endLineNumber);

    writer.pushSpan(span, lineStartOffset, indentedCodeBlock.getEndOffset());
    writer.pushSpan(spanPool.monospaceTypeface(), indentedCodeBlock.getStartOffset(), indentedCodeBlock.getEndOffset());
  }

  public void highlightStrikeThrough(Strikethrough strikethrough) {
    writer.pushSpan(spanPool.strikethrough(), strikethrough.getStartOffset(), strikethrough.getEndOffset());
    highlightMarkdownSyntax(strikethrough);
  }

//  public void visit(Superscript superscript) {
//    //writer.pushSpan(spanPool.foregroundColor(syntaxColor), superscript.getStartOffset(), superscript.getEndOffset());
//    //writer.pushSpan(new SuperscriptSpan(), superscript.getStartOffset(), superscript.getEndOffset());
//
//    BasedSequence superscriptText = superscript.getText();
//    int relativeStartOffset = superscript.getStartOffset();
//    int relativeEndOffset = superscript.getEndOffset() - 1;
//
//    Timber.i("-----------------------");
//    Timber.i("Superscript: [%s..%s]", relativeStartOffset, relativeEndOffset);
//
//    for (int i = superscriptText.length() - 1, o = relativeEndOffset; i >= 0 && o >= relativeStartOffset; o--, i--) {
//      char c = superscriptText.charAt(i);
//      if (c == '^') {
//        //Timber.i("Superscript: [%s..%s]", i + relativeStartOffset, relativeEndOffset);
//        //Timber.i("Superscript: %s", superscriptText.substring(i, superscriptText.length()));
//        Timber.i("[%s..%s]", o - 1, relativeEndOffset);
//        writer.pushSpan(new SuperscriptSpan(), o - 1, relativeEndOffset);
//        writer.pushSpan(spanPool.foregroundColor(syntaxColor), o - 1, relativeEndOffset);
//      }
//    }
//
//    writer.pushSpan(new SuperscriptSpan(), relativeStartOffset, relativeEndOffset);
//    writer.pushSpan(spanPool.foregroundColor(syntaxColor), relativeStartOffset, relativeEndOffset);
//
//    //    applyHighlightForegroundSpan(superscript);
//
//  }

  public void highlightBlockQuote(BlockQuote blockQuote) {
    // Android seems to require quote spans to be inserted at the starting of the line.
    // Otherwise, nested quote spans aren't rendered correctly. Calculate the offset for
    // this quote's starting index instead and include all text from there under the spans.
    int nestedParents = 0;
    Node parent = blockQuote.getParent();
    while (parent instanceof BlockQuote) {
      ++nestedParents;
      parent = parent.getParent();
    }

    CustomQuoteSpan quoteSpan = spanPool.quote(blockQuoteIndentationRuleColor, textBlockIndentationMargin, blockQuoteVerticalRuleStrokeWidth);
    writer.pushSpan(quoteSpan, blockQuote.getStartOffset() - nestedParents, blockQuote.getEndOffset());

    // Quote's vertical rule.
    writer.pushSpan(spanPool.foregroundColor(blockQuoteTextColor), blockQuote.getStartOffset() - nestedParents, blockQuote.getEndOffset());

    // Quote markers ('>').
    int markerStartOffset = blockQuote.getStartOffset() - nestedParents;
    writer.pushSpan(spanPool.foregroundColor(syntaxColor), markerStartOffset, blockQuote.getStartOffset() + 1);
  }

  public void highlightHeading(Heading heading) {
    writer.pushSpan(spanPool.relativeSize(HEADING_SIZES[heading.getLevel() - 1]), heading.getStartOffset(), heading.getEndOffset());
    writer.pushSpan(spanPool.bold(), heading.getStartOffset(), heading.getEndOffset());
    writer.pushSpan(
        spanPool.foregroundColor(syntaxColor),
        heading.getStartOffset(),
        heading.getStartOffset() + heading.getOpeningMarker().length()
    );
  }

  public void highlightListBlock(ListBlock listBlock) {
    writer.pushSpan(spanPool.leadingMargin(textBlockIndentationMargin), listBlock.getStartOffset(), listBlock.getEndOffset());
  }

  public void highlightListItem(ListItem listItem) {
    writer.pushSpan(spanPool.foregroundColor(syntaxColor), listItem.getStartOffset(), listItem.getStartOffset() + 1);
  }

  public void highlightThematicBreak(ThematicBreak thematicBreak) {
    BasedSequence thematicBreakChars = thematicBreak.getChars();

    // '****' clashes with bold syntax, so avoid drawing a rule for it.
    boolean canDrawHorizontalRule = !FOUR_ASTERISKS_HORIZONTAL_RULE.equals(thematicBreakChars);
    if (canDrawHorizontalRule) {
      HorizontalRuleSpan.Mode ruleMode;
      char firstChar = thematicBreakChars.charAt(0);
      if (firstChar == '*') {
        ruleMode = HorizontalRuleSpan.Mode.ASTERISKS;
      } else if (firstChar == '-') {
        ruleMode = HorizontalRuleSpan.Mode.HYPHENS;
      } else if (firstChar == '_') {
        ruleMode = HorizontalRuleSpan.Mode.UNDERSCORES;
      } else {
        throw new UnsupportedOperationException("Unknown thematic break mode: " + thematicBreakChars);
      }

      // Caching mutable BasedSequence isn't a good idea.
      String immutableThematicBreakChars = thematicBreakChars.toString();

      HorizontalRuleSpan hrSpan = spanPool.horizontalRule(immutableThematicBreakChars, horizontalRuleColor, horizontalRuleStrokeWidth, ruleMode);
      writer.pushSpan(hrSpan, thematicBreak.getStartOffset(), thematicBreak.getEndOffset());
    }

    writer.pushSpan(spanPool.foregroundColor(syntaxColor), thematicBreak.getStartOffset(), thematicBreak.getEndOffset());
  }

  private void highlightThematicBreakWithoutLeadingNewLine(Heading node) {
    if (!node.isSetextHeading()) {
      throw new AssertionError();
    }

    int ruleStartOffset = node.getStartOffset() + node.getText().length() + 1;
    BasedSequence thematicBreakChars = node.getChars().subSequence(ruleStartOffset, node.getEndOffset());

    if (thematicBreakChars.charAt(0) == '=') {
      // "===" line breaks aren't supported by Reddit.
      return;
    }

    // '****' clashes with bold syntax, so avoid drawing a rule for it.
    boolean canDrawHorizontalRule = !FOUR_ASTERISKS_HORIZONTAL_RULE.equals(thematicBreakChars);
    if (canDrawHorizontalRule) {
      HorizontalRuleSpan horizontalRuleSpan = spanPool.horizontalRule(
          thematicBreakChars,
          horizontalRuleColor,
          horizontalRuleStrokeWidth,
          HorizontalRuleSpan.Mode.HYPHENS
      );
      writer.pushSpan(horizontalRuleSpan, ruleStartOffset, node.getEndOffset());
    }
    writer.pushSpan(spanPool.foregroundColor(syntaxColor), ruleStartOffset, node.getEndOffset());
  }

  public void highlightLink(Link link) {
    // First, link text.
    writer.pushSpan(spanPool.foregroundColor(syntaxColor), link.getStartOffset(), link.getStartOffset() + 1);
    int linkTextClosingMarkerPosition = link.getStartOffset() + link.getText().length() + 1;
    writer.pushSpan(spanPool.foregroundColor(syntaxColor), linkTextClosingMarkerPosition, linkTextClosingMarkerPosition + 1);

    // Next, link URL.
    int linkUrlOpeningMarkerPosition = linkTextClosingMarkerPosition + 1;
    writer.pushSpan(spanPool.foregroundColor(linkUrlColor), linkUrlOpeningMarkerPosition, link.getEndOffset());
  }
}
