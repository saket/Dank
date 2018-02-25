package me.saket.dank.utils;

import android.graphics.Color;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextPaint;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.Submission;

import org.sufficientlysecure.htmltextview.HtmlTagHandler;
import org.xml.sax.XMLReader;

import javax.inject.Inject;

import dagger.Lazy;
import me.saket.dank.markdownhints.MarkdownSpanPool;
import me.saket.dank.ui.submission.PendingSyncReply;
import me.saket.dank.widgets.span.HorizontalRuleSpan;

/**
 * Handles converting Reddit's markdown into Spans that can be rendered by TextView.
 * <p>
 * TODO: Proivide using Dagger.
 * TODO: Use MarkdownHints through this class.
 */
@SuppressWarnings({"StatementWithEmptyBody", "deprecation"})
public class Markdown {

  private static final TextPaint EMPTY_TEXTPAINT = new TextPaint();

  private final Lazy<MarkdownSpanPool> spanPool;

  @Inject
  public Markdown(Lazy<MarkdownSpanPool> spanPool) {
    this.spanPool = spanPool;
  }

  private CharSequence parse(String textWithMarkdown) {
    RedditMarkdownHtmlHandler htmlTagHandler = new RedditMarkdownHtmlHandler(null);
    String source = Html.fromHtml(textWithMarkdown).toString();

    try {
      String sourceWithCustomTags = htmlTagHandler.overrideTags(source);
      Spanned spanned = Html.fromHtml(sourceWithCustomTags, null, htmlTagHandler);
      return trimTrailingWhitespace(spanned);

    } catch (Exception e) {
      e.printStackTrace();
      return Html.fromHtml(source);
    }
  }

  // TODO: Use Flexmark for markdown.
  public CharSequence parse(Message message) {
    String messageBodyWithHtml = JrawUtils.messageBodyHtml(message);
    return parse(messageBodyWithHtml);
  }

  // TODO.
  public CharSequence parse(Comment comment) {
    return comment.getBody();
  }

  // TODO.
  public CharSequence parse(PendingSyncReply reply) {
    return reply.body();
  }

  // TODO.
  public CharSequence parseSelfText(Submission submission) {
    //String selfTextHtml = submission.getDataNode().get("selftext_html").asText(submission.getSelftext() /* defaultValue */)
    return submission.getSelftext().trim();
  }

  /**
   * Reddit sends escaped body: "JRAW is escaping html entities.\n\n&lt; &gt; &amp;"
   * instead of "JRAW is escaping html entities.\n\n< > &".
   *
   * Convert "**Something**" -> "Something", without any styling.
   */
  public String stripMarkdown(String markdown) {
    // TODO: Use non-html bodies sent by Reddit instead?
    // Since all styling is added using spans, converting the CharSequence to a String will remove all styling.
    CharSequence result;
    RedditMarkdownHtmlHandler htmlTagHandler = new RedditMarkdownHtmlHandler(EMPTY_TEXTPAINT);
    String source = Html.fromHtml(markdown).toString();

    try {
      String sourceWithCustomTags = htmlTagHandler.overrideTags(source);
      Spanned spanned = Html.fromHtml(sourceWithCustomTags, null, htmlTagHandler);
      result = trimTrailingWhitespace(spanned);

    } catch (Exception e) {
      e.printStackTrace();
      result = Html.fromHtml(source);
    }
    return result.toString();
  }

  private static CharSequence trimTrailingWhitespace(CharSequence source) {
    if (source == null) {
      return null;
    }

    int len = source.length();
    while (--len >= 0 && Character.isWhitespace(source.charAt(len))) {
    }
    return source.subSequence(0, len + 1);
  }

  private static class RedditMarkdownHtmlHandler extends HtmlTagHandler {

    public RedditMarkdownHtmlHandler(TextPaint textPaint) {
      super(textPaint);
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

}
