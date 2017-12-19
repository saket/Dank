package me.saket.dank.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.LineBackgroundSpan;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.Submission;

import org.sufficientlysecure.htmltextview.HtmlTagHandler;
import org.xml.sax.XMLReader;

import javax.inject.Inject;

import me.saket.dank.ui.submission.PendingSyncReply;

/**
 * Handles converting Reddit's markdown into Spans that can be rendered by TextView.
 * <p>
 * TODO: Proivide using Dagger.
 * TODO: Use MarkdownHints through this class.
 */
@SuppressWarnings({ "StatementWithEmptyBody", "deprecation" })
public class Markdown {

  private static final TextPaint EMPTY_TEXTPAINT = new TextPaint();

  @Inject
  public Markdown() {
  }

  // TODO: Use Flexmark for markdown.
  public CharSequence parse(Message message) {
    return message.getBody();
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
  public String parseSelfText(Submission submission) {
    //String selfTextHtml = submission.getDataNode().get("selftext_html").asText(submission.getSelftext() /* defaultValue */)
    return submission.getSelftext();
  }

  public static CharSequence parseRedditMarkdownHtml(String markdown, TextPaint textPaint) {
    RedditMarkdownHtmlHandler htmlTagHandler = new RedditMarkdownHtmlHandler(textPaint);
    String source = Html.fromHtml(markdown).toString();

    try {
      String sourceWithCustomTags = htmlTagHandler.overrideTags(source);
      Spanned spanned = Html.fromHtml(sourceWithCustomTags, null, htmlTagHandler);
      return trimTrailingWhitespace(spanned);

    } catch (Exception e) {
      e.printStackTrace();
      return Html.fromHtml(source);
    }
  }

  /**
   * Convert "**Something**" -> "Something", without any styling.
   */
  public static String stripMarkdown(String markdown) {
    // Since all the styling is added using spans, converting the CharSequence to a String will remove all the styling.
    return parseRedditMarkdownHtml(markdown, EMPTY_TEXTPAINT).toString();
  }

  private static CharSequence trimTrailingWhitespace(CharSequence source) {
    if (source == null) {
      return null;
    }

    // Loop back to the first non-whitespace character.
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

    /**
     * {@link LineBackgroundSpan} is used for drawing spans that cover the entire line.
     */
    public static class HorizontalRuleSpan implements LineBackgroundSpan {
      private final int ruleColor;          // Color of line
      private final float line;         // Line size

      public HorizontalRuleSpan(int ruleColor, float lineHeight) {
        this.ruleColor = ruleColor;
        this.line = lineHeight;
      }

      @Override
      public void drawBackground(Canvas canvas, Paint paint, int left, int right, int top, int baseline, int bottom, CharSequence text,
          int start, int end, int lnum)
      {
        int originalPaintColor = paint.getColor();
        float y = (float) (top + (bottom - top) / 2) - (line / 2);
        RectF lineRect = new RectF(left, y, (right - left), y + line);
        paint.setColor(ruleColor);
        canvas.drawRect(lineRect, paint);
        paint.setColor(originalPaintColor);
      }
    }
  }
}
