package me.saket.dank.utils.markdown;

import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;

import com.commonsware.cwac.anddown.AndDown;
import com.nytimes.android.external.cache3.Cache;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.Submission;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.exceptions.Exceptions;
import me.saket.dank.BuildConfig;
import me.saket.dank.ui.submission.PendingSyncReply;
import me.saket.dank.utils.JrawUtils;

/**
 * Handles converting Reddit's markdown into Spans that can be rendered by TextView.
 * <p>
 * TODO: Use MarkdownHints through this class.
 */
@SuppressWarnings({ "StatementWithEmptyBody", "deprecation" })
public class Markdown {

  private final DankHtmlTagHandler htmlTagHandler;
  private final AndDown andDown;
  private final Cache<String, CharSequence> fromHtmlCache;
  private final Cache<String, String> fromMarkdownCache;

  @Inject
  public Markdown(
      DankHtmlTagHandler htmlTagHandler,
      AndDown andDown,
      @Named("markdown_from_html") Cache<String, CharSequence> fromHtmlCache,
      @Named("markdown_from_markdown") Cache<String, String> fromMarkdownCache)
  {
    this.htmlTagHandler = htmlTagHandler;
    this.andDown = andDown;
    this.fromHtmlCache = fromHtmlCache;
    this.fromMarkdownCache = fromMarkdownCache;
  }

  private CharSequence parseHtml(String textWithHtml, boolean escapeHtml, boolean escapeForwardSlashes) {
    Callable<CharSequence> valueSeeder = () -> {
      String source = textWithHtml;

      if (escapeHtml) {
        source = Html.fromHtml(source).toString();
      }
      if (escapeForwardSlashes) {
        // Forward slashes need to be escaped. I don't know a better way to do this.
        // Converts ¯\\_(ツ)_/¯ -> ¯\_(ツ)_/¯.
        source = source.replaceAll(Matcher.quoteReplacement("\\\\"), Matcher.quoteReplacement("\\"));
      }

      try {
        String sourceWithCustomTags = htmlTagHandler.overrideTags(source);
        Spannable spannable = (Spannable) Html.fromHtml(sourceWithCustomTags, null, htmlTagHandler);
        spannable = htmlTagHandler.replaceBlockQuoteSpans(spannable);
        return trimTrailingWhitespace(spannable);

      } catch (Exception e) {
        e.printStackTrace();
        return Html.fromHtml(source);
      }
    };
    try {
      return fromHtmlCache.get(textWithHtml, valueSeeder);
    } catch (ExecutionException e) {
      // Should never happen.
      throw Exceptions.propagate(e);
    }
  }

  private CharSequence parseMarkdown(String textWithMarkdown, boolean escapeHtml, boolean escapeForwardSlashes) {
    Callable<String> valueSeeder = () -> andDown.markdownToHtml(
        textWithMarkdown,
        AndDown.HOEDOWN_EXT_FENCED_CODE
            | AndDown.HOEDOWN_EXT_STRIKETHROUGH
            | AndDown.HOEDOWN_EXT_UNDERLINE
            | AndDown.HOEDOWN_EXT_HIGHLIGHT
            | AndDown.HOEDOWN_EXT_QUOTE
            | AndDown.HOEDOWN_EXT_SUPERSCRIPT
            | AndDown.HOEDOWN_EXT_SPACE_HEADERS
            | AndDown.HOEDOWN_EXT_TABLES,
        0);

    try {
      String markdownToHtml = fromMarkdownCache.get(textWithMarkdown, valueSeeder);
      return parseHtml(markdownToHtml, escapeHtml, escapeForwardSlashes);

    } catch (ExecutionException e) {
      // Should never happen.
      throw Exceptions.propagate(e);
    }
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

  /**
   * Reddit sends escaped body: "JRAW is escaping html entities.\n\n&lt; &gt; &amp;"
   * instead of "JRAW is escaping html entities.\n\n< > &".
   * <p>
   * Convert "**Something**" -> "Something", without any styling.
   */
  public String stripMarkdownFromHtml(String textWithHtml) {
    // Since all HTML styling is added using spans, converting the CharSequence to a String will remove all styling.
    String sourceWithoutHtml = Html.fromHtml(textWithHtml).toString();

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

  private static CharSequence trimTrailingWhitespace(CharSequence source) {
    int len = source.length();
    while (--len >= 0 && Character.isWhitespace(source.charAt(len))) {
    }
    return source.subSequence(0, len + 1);
  }

  public void clearCache() {
    if (!BuildConfig.DEBUG) {
      throw new AssertionError();
    }
    fromHtmlCache.invalidateAll();
  }
}
