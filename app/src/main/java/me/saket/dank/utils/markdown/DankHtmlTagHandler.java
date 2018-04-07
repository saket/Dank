package me.saket.dank.utils.markdown;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.QuoteSpan;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.XMLReader;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import me.saket.dank.markdownhints.MarkdownHintOptions;
import me.saket.dank.markdownhints.MarkdownSpanPool;
import me.saket.dank.markdownhints.spans.CustomQuoteSpan;
import me.saket.dank.markdownhints.spans.HorizontalRuleSpan;
import timber.log.Timber;

public class DankHtmlTagHandler extends HtmlTagHandler {

  private final MarkdownHintOptions options;
  private final MarkdownSpanPool spanPool;

  @Inject
  public DankHtmlTagHandler(MarkdownHintOptions options, MarkdownSpanPool spanPool) {
    this.options = options;
    this.spanPool = spanPool;
  }

  @Override
  protected NumberSpan numberSpan(int number) {
    return new NumberSpan(number, options.textBlockIndentationMargin());
  }

  /**
   * See {@link HtmlTagHandler#overrideTags(String)}. This exists because that method is not public.
   */
  String overrideTags(@Nullable String html) {
    //noinspection ConstantConditions
    String htmlWithoutParagraphsInList = html
        // Reddit sends paragraphs inside <li> items, which doesn't make sense?
        .replace("<p>", "<div>")
        .replace("</p>", "</div>")
        .replace("<li>\\s*<div>", "<li>")
        .replace("</div>\\s*</li>", "</li>")
        .replace("<li><div>", "<li>")
        .replace("</div></li>", "</li>");

    if (html.contains("<blockquote>")) {
      // Not sure if it's Android or Dank's HTML parser, but lists
      // inside block-quotes aren't rendered very nicely.
      htmlWithoutParagraphsInList = normalizeListsInsideBlockQuotes(htmlWithoutParagraphsInList);
    }

    return htmlWithoutParagraphsInList
        // Wrap within a division to add spacing around the HR.
        .replace("<hr/>", "<div><hr/></div>")

        // Progressive reduction of text
        .replace("<sup>", "<sup><small>")
        .replace("</sup>", "</small></sup>")

        // WARNING: THESE SHOULD BE AT THE LAST.
        .replace("<ul", "<" + HtmlTagHandler.UNORDERED_LIST)
        .replace("</ul>", "</" + HtmlTagHandler.UNORDERED_LIST + ">")
        .replace("<ol", "<" + HtmlTagHandler.ORDERED_LIST)
        .replace("</ol>", "</" + HtmlTagHandler.ORDERED_LIST + ">")
        .replace("<li", "<" + HtmlTagHandler.LIST_ITEM)
        .replace("</li>", "</" + HtmlTagHandler.LIST_ITEM + ">")
        ;
  }

  public Spannable replaceBlockQuoteSpans(Spannable spannable) {
    QuoteSpan[] quoteSpans = spannable.getSpans(0, spannable.length(), QuoteSpan.class);
    for (QuoteSpan quoteSpan : quoteSpans) {
      CustomQuoteSpan customQuoteSpan = spanPool.quote(
          options.blockQuoteIndentationRuleColor(),
          options.textBlockIndentationMargin(),
          options.blockQuoteVerticalRuleStrokeWidth());

      int start = spannable.getSpanStart(quoteSpan);
      int end = spannable.getSpanEnd(quoteSpan);
      spannable.setSpan(customQuoteSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

      spannable.removeSpan(quoteSpan);
    }
    return spannable;
  }

  @Override
  public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
    if (tag.equals("hr") && !opening) {
      handleHRTag(output);
    } else {
      super.handleTag(opening, tag, output, xmlReader);
    }
  }

  private void handleHRTag(Editable output) {
    int start = output.length();

    // Paragraph styles like LineBackgroundSpan need to end with a new line.
    output.append(" \n");

    HorizontalRuleSpan hrSpan = spanPool.horizontalRule(
        "",
        options.horizontalRuleColor(),
        options.horizontalRuleStrokeWidth(),
        HorizontalRuleSpan.Mode.HYPHENS);
    output.setSpan(hrSpan, start, output.length(), 0);
  }

  @VisibleForTesting
  static String normalizeListsInsideBlockQuotes(String html) {
    try {
      Document document = Jsoup.parse(html);
      document.outputSettings(new Document.OutputSettings().prettyPrint(false));

      Elements blockQuotes = document.select("blockquote");
      for (Element blockQuote : blockQuotes) {
        String quoteContent = blockQuote.toString();
        if (quoteContent.contains("<ol") || quoteContent.contains("<ul")) {
          String quoteWithEscapedSingleQuotes = escapeSingleQuotes(quoteContent);
          int indexOfContentToBeModified = html.indexOf(quoteWithEscapedSingleQuotes);
          int lengthOfContentToBeModified = quoteWithEscapedSingleQuotes.length();

          String quoteContentWithoutLists = normalizeList(quoteContent);
          quoteContentWithoutLists = escapeSingleQuotes(quoteContentWithoutLists);

          html = html.substring(0, indexOfContentToBeModified)
              + quoteContentWithoutLists
              + html.substring(indexOfContentToBeModified + lengthOfContentToBeModified);
        }
      }
    } catch (Exception e) {
      Timber.e(e, "Couldn't normalize list inside blockquotes: " + html);
    }
    return html;
  }

  static String escapeSingleQuotes(String s) {
    return s.replaceAll("'", "&#39;");
  }

  /**
   * Copied from Slide app. I have no trust on this code.
   */
  private static String normalizeList(String html) {
    int firstIndex;
    boolean isNumbered;
    int firstOl = html.indexOf("<ol");
    int firstUl = html.indexOf("<ul");

    if ((firstUl != -1 && firstOl > firstUl) || firstOl == -1) {
      firstIndex = firstUl;
      isNumbered = false;
    } else {
      firstIndex = firstOl;
      isNumbered = true;
    }
    List<Integer> listNumbers = new ArrayList<>();
    int indent = -1;

    int i = firstIndex;
    while (i < html.length() - 4 && i != -1) {
      if (html.substring(i, i + 3).equals("<ol") || html.substring(i, i + 3).equals("<ul")) {
        if (html.substring(i, i + 3).equals("<ol")) {
          isNumbered = true;
          indent++;
          listNumbers.add(indent, 1);
        } else {
          isNumbered = false;
        }
        i = html.indexOf("<li", i);
      } else if (html.substring(i, i + 3).equals("<li")) {
        int tagEnd = html.indexOf(">", i);
        int itemClose = html.indexOf("</li", tagEnd);
        int ulClose = html.indexOf("<ul", tagEnd);
        int olClose = html.indexOf("<ol", tagEnd);
        int closeTag;

        // Find what is closest: </li>, <ul>, or <ol>
        if (((ulClose == -1 && itemClose != -1) || (itemClose != -1 && ulClose != -1 && itemClose < ulClose)) && ((olClose == -1 && itemClose != -1) || (itemClose != -1 && olClose != -1 && itemClose < olClose))) {
          closeTag = itemClose;
        } else if (((ulClose == -1 && olClose != -1) || (olClose != -1 && ulClose != -1 && olClose < ulClose)) && ((olClose == -1 && itemClose != -1) || (olClose != -1 && itemClose != -1 && olClose < itemClose))) {
          closeTag = olClose;
        } else {
          closeTag = ulClose;
        }

        String text = html.substring(tagEnd + 1, closeTag);
        String indentSpacing = "";
        for (int j = 0; j < indent; j++) {
          indentSpacing += "&nbsp;&nbsp;&nbsp;&nbsp;";
        }
        if (isNumbered) {
          html = html.substring(0, tagEnd + 1)
              + indentSpacing +
              listNumbers.get(indent) + ". " +
              text + "<br/>" +
              html.substring(closeTag);
          listNumbers.set(indent, listNumbers.get(indent) + 1);
          i = closeTag + 3;
        } else {
          html = html.substring(0, tagEnd + 1) + indentSpacing + "• " + text + "<br/>" + html.substring(closeTag);
          i = closeTag + 2;
        }
      } else {
        i = html.indexOf("<", i + 1);
        if (i != -1 && html.substring(i, i + 4).equals("</ol")) {
          indent--;
          if (indent == -1) {
            isNumbered = false;
          }
        }
      }
    }

    html = html.replace("<ol>", "").replace("<ul>", "").replace("<li>", "").replace("</li>", "").replace("</ol>", "").replace("</ul>", ""); //Remove the tags, which actually work in Android 7.0 on

    return html;
  }
}
