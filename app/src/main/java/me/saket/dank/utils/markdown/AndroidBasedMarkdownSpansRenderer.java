package me.saket.dank.utils.markdown;

import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;

import com.nytimes.android.external.cache3.Cache;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.Submission;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import javax.inject.Named;

import io.reactivex.exceptions.Exceptions;
import me.saket.dank.BuildConfig;
import me.saket.dank.ui.submission.PendingSyncReply;
import me.saket.dank.utils.JrawUtils;

public class AndroidBasedMarkdownSpansRenderer implements Markdown {

  private final DankHtmlTagHandler htmlTagHandler;
  private final Cache<String, CharSequence> htmlToSpansCache;
  private final Cache<String, String> markdownToHtmlCache;

  public AndroidBasedMarkdownSpansRenderer(
      DankHtmlTagHandler htmlTagHandler,
      @Named("markdown_from_html") Cache<String, CharSequence> htmlToSpansCache,
      @Named("markdown_from_markdown") Cache<String, String> markdownToHtmlCache)
  {
    this.htmlTagHandler = htmlTagHandler;
    this.htmlToSpansCache = htmlToSpansCache;
    this.markdownToHtmlCache = markdownToHtmlCache;
  }

  private CharSequence parseHtml(String html, boolean escapeHtml, boolean escapeForwardSlashes) {
    Callable<CharSequence> valueSeeder = () -> {
      String source = html;

      if (escapeHtml) {
        source = Html.fromHtml(source).toString();
      }
      if (escapeForwardSlashes) {
        // Forward slashes need to be escaped. I don't know a better way to do this.
        // Converts ¯\\_(ツ)_/¯ -> ¯\_(ツ)_/¯.
        source = source.replaceAll(Matcher.quoteReplacement("\\\\"), Matcher.quoteReplacement("\\"));
      }

      try {
        source = org.jsoup.parser.Parser.unescapeEntities(source, true);
        source = htmlTagHandler.overrideTags(source);

        Spannable spannable = (Spannable) Html.fromHtml(source, null, htmlTagHandler);
        spannable = htmlTagHandler.replaceBlockQuoteSpans(spannable);
        return trimTrailingWhitespace(spannable);

      } catch (Exception e) {
        e.printStackTrace();
        return Html.fromHtml(source);
      }
    };

    try {
      if (true) {
        try {
          return valueSeeder.call();
        } catch (Exception e) {
          e.printStackTrace();
          return "";
        }
      } else {
        return htmlToSpansCache.get(html, valueSeeder);
      }

    } catch (ExecutionException e) {
      // Should never happen.
      throw Exceptions.propagate(e);
    }
  }

  private CharSequence parseMarkdown(String textWithMarkdown, boolean escapeHtml, boolean escapeForwardSlashes) {
//    Callable<String> valueSeeder = () -> andDown.markdownToHtml(
//        textWithMarkdown,
//        AndDown.HOEDOWN_EXT_FENCED_CODE
//            | AndDown.HOEDOWN_EXT_STRIKETHROUGH
//            | AndDown.HOEDOWN_EXT_UNDERLINE
//            | AndDown.HOEDOWN_EXT_HIGHLIGHT
//            | AndDown.HOEDOWN_EXT_QUOTE
//            | AndDown.HOEDOWN_EXT_SUPERSCRIPT
//            | AndDown.HOEDOWN_EXT_SPACE_HEADERS
//            | AndDown.HOEDOWN_EXT_TABLES,
//        0);
//
//    try {
//      String markdownToHtml = markdownToHtmlCache.get(textWithMarkdown, valueSeeder);
//      return parseHtml(markdownToHtml, escapeHtml, escapeForwardSlashes);
//
//    } catch (ExecutionException e) {
//      // Should never happen.
//      throw Exceptions.propagate(e);
//    }
    throw new UnsupportedOperationException();
  }

  public CharSequence parse(PendingSyncReply reply) {
    return parseMarkdown(reply.body(), false, true);
  }

  public CharSequence parseAuthorFlair(String flair) {
    return parseHtml(flair, true, false);
  }

  public CharSequence parse(Message message) {
    return parseHtml(JrawUtils.messageBodyHtml(message), true, false);
  }

  public CharSequence parse(Comment comment) {
    return parseHtml(JrawUtils.commentBodyHtml(comment), true, false);
  }

  public CharSequence parseSelfText(Submission submission) {
    return parseHtml(JrawUtils.selfPostHtml(submission), true, false);
  }

  private String stripMarkdownFromHtml(String html) {
    // Since all HTML styling is added using spans, converting the CharSequence to a String will remove all styling.
    String sourceWithoutHtml = Html.fromHtml(html).toString();

    CharSequence result;
    try {
      String sourceWithCustomTags = htmlTagHandler.overrideTags(sourceWithoutHtml);
      Spanned spanned = Html.fromHtml(sourceWithCustomTags, null, htmlTagHandler);
      result = trimTrailingWhitespace(spanned);

    } catch (Exception e) {
      e.printStackTrace();
      result = Html.fromHtml(sourceWithoutHtml);
    }
    return result.toString();
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public String stripMarkdown(Comment comment) {
    return stripMarkdownFromHtml(JrawUtils.commentBodyHtml(comment));
  }

  /**
   * See {@link #stripMarkdown(Comment)}.
   */
  @Override
  public String stripMarkdown(Message message) {
    return stripMarkdownFromHtml(JrawUtils.messageBodyHtml(message));
  }

  private static CharSequence trimTrailingWhitespace(CharSequence source) {
    int len = source.length();
    //noinspection StatementWithEmptyBody
    while (--len >= 0 && Character.isWhitespace(source.charAt(len))) {
    }
    return source.subSequence(0, len + 1);
  }

  public void clearCache() {
    if (!BuildConfig.DEBUG) {
      throw new AssertionError();
    }
    markdownToHtmlCache.invalidateAll();
    htmlToSpansCache.invalidateAll();
  }
}
