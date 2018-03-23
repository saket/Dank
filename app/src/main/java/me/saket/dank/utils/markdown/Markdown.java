package me.saket.dank.utils.markdown;

import android.text.Html;
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
  private final Cache<String, CharSequence> cache;

  @Inject
  public Markdown(DankHtmlTagHandler htmlTagHandler, AndDown andDown, @Named("markdown") Cache<String, CharSequence> markdownCache) {
    this.htmlTagHandler = htmlTagHandler;
    this.andDown = andDown;
    this.cache = markdownCache;
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
        Spanned spanned = Html.fromHtml(sourceWithCustomTags, null, htmlTagHandler);
        return trimTrailingWhitespace(spanned);

      } catch (Exception e) {
        e.printStackTrace();
        return Html.fromHtml(source);
      }
    };
    try {
      return cache.get(textWithHtml, valueSeeder);
    } catch (ExecutionException e) {
      // Should never happen.
      throw Exceptions.propagate(e);
    }
  }

  public CharSequence parseMarkdown(String textWithMarkdown, boolean escapeForwardSlashes) {
    return parseHtml(andDown.markdownToHtml(textWithMarkdown), false, escapeForwardSlashes);
  }

  public CharSequence parse(PendingSyncReply reply) {
    return parseMarkdown(reply.body(), true);
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
  // TODO: cache.
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
    cache.invalidateAll();
  }
}
