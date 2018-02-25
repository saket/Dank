package me.saket.dank.utils.markdown;

import android.graphics.Color;
import android.support.annotation.Nullable;
import android.text.Editable;

import org.xml.sax.XMLReader;

import javax.inject.Inject;

import me.saket.dank.markdownhints.MarkdownHintOptions;
import me.saket.dank.widgets.span.HorizontalRuleSpan;

public class DankHtmlTagHandler extends HtmlTagHandler {

  private final MarkdownHintOptions options;

  @Inject
  public DankHtmlTagHandler(MarkdownHintOptions options) {
    this.options = options;
  }

  @Override
  protected NumberSpan createNumberSpan(int number) {
    return new NumberSpan(number, options.textBlockIndentationMargin());
  }

  /**
   * See {@link HtmlTagHandler#overrideTags(String)}. This exists because that method is not public.
   */
  String overrideTags(@Nullable String html) {
    //noinspection ConstantConditions
    return html
        .replace("<ul", "<" + HtmlTagHandler.UNORDERED_LIST)
        .replace("</ul>", "</" + HtmlTagHandler.UNORDERED_LIST + ">")
        .replace("<ol", "<" + HtmlTagHandler.ORDERED_LIST)
        .replace("</ol>", "</" + HtmlTagHandler.ORDERED_LIST + ">")
        .replace("<li", "<" + HtmlTagHandler.LIST_ITEM)
        .replace("</li>", "</" + HtmlTagHandler.LIST_ITEM + ">")
        .replace("<hr/>", "<div><hr/></div>")   // Wrap within a division to add spacing around the HR.
        ;
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
    output.append(" \n");   // Paragraph styles like LineBackgroundSpan need to end with a new line.
    output.setSpan(new HorizontalRuleSpan(Color.DKGRAY, 4f), start, output.length(), 0);
  }
}
