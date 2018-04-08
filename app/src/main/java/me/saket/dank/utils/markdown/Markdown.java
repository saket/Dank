package me.saket.dank.utils.markdown;

import android.support.annotation.VisibleForTesting;
import android.text.Html;

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
import me.saket.dank.utils.SafeFunction;

/**
 * Handles converting Reddit's markdown into Spans that can be rendered by TextView.
 * <p>
 * TODO: Use MarkdownHints through this class.
 */
@SuppressWarnings({ "StatementWithEmptyBody", "deprecation" })
public class Markdown {

  private final SafeFunction<String, String> markdownToHtmlParser;
  private final HtmlToSpansParser htmlToSpanParser;
  private final Cache<String, CharSequence> fromHtmlCache;
  private final Cache<String, String> fromMarkdownCache;

  @Inject
  public Markdown(
      HtmlToSpansParser htmlToSpanParser,
      @Named("markdown_to_html") SafeFunction<String, String> markdownToHtmlParser,
      @Named("markdown_from_html") Cache<String, CharSequence> fromHtmlCache,
      @Named("markdown_from_markdown") Cache<String, String> fromMarkdownCache)
  {
    this.htmlToSpanParser = htmlToSpanParser;
    this.markdownToHtmlParser = markdownToHtmlParser;
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
      return htmlToSpanParser.parse(source);
    };
    try {
      // TODO: 07/04/18 REMOVEE!
      try {
        return valueSeeder.call();
      } catch (Exception e) {
        e.printStackTrace();
        return "";
      }
    } catch (Exception e) {
      // Should never happen.
      throw Exceptions.propagate(e);
    }
  }

  @VisibleForTesting
  CharSequence parseMarkdown(String textWithMarkdown, boolean escapeHtml, boolean escapeForwardSlashes) {
    Callable<String> valueSeeder = () -> markdownToHtmlParser.apply(textWithMarkdown);
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
  public String stripMarkdownFromHtml(String html) {
    return htmlToSpanParser.stripMarkdownFromHtml(html);
  }

  public void clearCache() {
    if (!BuildConfig.DEBUG) {
      throw new AssertionError();
    }
    fromHtmlCache.invalidateAll();
  }
}
