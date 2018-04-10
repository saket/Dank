package me.saket.dank.utils.markdown.markwon;

import android.support.annotation.VisibleForTesting;

import com.nytimes.android.external.cache3.Cache;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.Submission;

import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;

import java.util.Arrays;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.exceptions.Exceptions;
import me.saket.dank.BuildConfig;
import me.saket.dank.ui.submission.PendingSyncReply;
import me.saket.dank.utils.markdown.Markdown;
import ru.noties.markwon.SpannableConfiguration;
import ru.noties.markwon.renderer.SpannableRenderer;
import ru.noties.markwon.tasklist.TaskListExtension;

public class MarkwonBasedMarkdownRenderer implements Markdown {

  private static final Pattern LINK_MARKDOWN_PATTERN = Pattern.compile("\\[(.*)\\]\\((.*)\\)");
  private static final Pattern HEADING_MARKDOWN_PATTERN = Pattern.compile("(#{1,6})\\s{0}([\\w\\s]*)");

  private final Cache<String, CharSequence> cache;
  private final Parser parser;
  private final SpannableConfiguration configuration;

  @Inject
  public MarkwonBasedMarkdownRenderer(
      SpannableConfiguration configuration,
      AutoRedditLinkExtension autoRedditLinkExtension,
      @Named("markwon_spans_renderer") Cache<String, CharSequence> cache)
  {
    this.cache = cache;
    this.configuration = configuration;

    this.parser = new Parser.Builder()
        .extensions(Arrays.asList(
            StrikethroughExtension.create(),
            TablesExtension.create(),
            TaskListExtension.create(),
            AutolinkExtension.create(),
            autoRedditLinkExtension
        ))
        .build();
  }

  private CharSequence parseMarkdown(String markdown) {
    // Convert '&lgt;' to '<', etc.
    markdown = org.jsoup.parser.Parser.unescapeEntities(markdown, true);
    markdown = fixInvalidTables(markdown);
    markdown = escapeSpacesInLinkUrls(markdown);
    markdown = fixInvalidHeadings(markdown);

    // WARNING: this should be at the end.
    markdown = new SuperscriptMarkdownToHtml().convert(markdown);

    // It's better **not** to re-use this class between multiple calls.
    SpannableRenderer renderer = new SpannableRenderer();
    Node node = parser.parse(markdown);
    return renderer.render(configuration, node);
  }

  CharSequence getOrParse(String markdown) {
    Callable<CharSequence> valueSeeder = () -> parseMarkdown(markdown);

    try {
      // TODO: remove.
      if (BuildConfig.DEBUG) {
        return valueSeeder.call();
      }
      return cache.get(markdown, valueSeeder);
    } catch (Exception e) {
      // Should never happen.
      throw Exceptions.propagate(e);
    }
  }

  @Override
  public CharSequence parse(PendingSyncReply reply) {
    return getOrParse(reply.body());
  }

  @Override
  public CharSequence parseAuthorFlair(String flair) {
    return getOrParse(flair);
  }

  @Override
  public CharSequence parse(Message message) {
    return getOrParse(message.getBody());
  }

  @Override
  public CharSequence parse(Comment comment) {
    return getOrParse(comment.getBody());
  }

  @Override
  public CharSequence parseSelfText(Submission submission) {
    return getOrParse(submission.getSelftext());
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public String stripMarkdown(Comment comment) {
    return parse(comment).toString();
  }

  /**
   * See {@link #stripMarkdown(Comment)}.
   */
  @Override
  public String stripMarkdown(Message message) {
    return parse(message).toString();
  }

  @Override
  public void clearCache() {
    if (!BuildConfig.DEBUG) {
      throw new AssertionError();
    }
    cache.invalidateAll();
  }

  /**
   * Markwon needs at-least three dashes for table headers.
   */
  @VisibleForTesting
  String fixInvalidTables(String markdown) {
    return markdown
        .replace(":--|", ":---|")
        .replace("|:--:|", "|:---:|")
        .replace("|:--", "|:---")
        .replace("|--:", "|---:")
        .replace("|-:", "|---:");
  }

  @VisibleForTesting
  String escapeSpacesInLinkUrls(String markdown) {
    Matcher matcher = LINK_MARKDOWN_PATTERN.matcher(markdown);

    while (matcher.find()) {
      String linkText = matcher.group(1);
      String linkUrl = matcher.group(2).replaceAll("\\s", "%20");

      markdown = markdown.substring(0, matcher.start())
          + String.format("[%s](%s)", linkText, linkUrl)
          + markdown.substring(matcher.end(), markdown.length());
      //markdown = markdown.replace(matcher.group(0), String.format("[%s](%s)", linkText, linkUrl));
    }

    return markdown;
  }

  /**
   * Ensures a space between '#' and heading text.
   */
  @VisibleForTesting
  String fixInvalidHeadings(String markdown) {
    Matcher matcher = HEADING_MARKDOWN_PATTERN.matcher(markdown);
    while (matcher.find()) {
      String heading = matcher.group(0);
      String hashes = matcher.group(1);
      String content = matcher.group(2).trim();
      markdown = markdown.replace(heading, String.format("%s %s", hashes, content));
    }

    return markdown;
  }

  /**
   * commonmark-java does not recognize '^'. This replaces all '^' with {@code <sup>} tags.
   */
  @VisibleForTesting
  static class SuperscriptMarkdownToHtml {
    public String convert(String html) {
      Stack<Character> stack = new Stack<>();
      StringBuilder builder = new StringBuilder(html.length());

      for (int i = 0; i < html.length(); i++) {
        char c = html.charAt(i);
        char nextC = (i + 1) < html.length() ? html.charAt(i + 1) : Character.MIN_VALUE;

        if (c == '^') {
          stack.add(c);
          builder.append("<sup>");
        } else {
          if (Character.isWhitespace(c) || (c == '\\' && nextC == 'n')) {
            flush(stack, builder);
          }
          builder.append(c);
        }
      }

      flush(stack, builder);
      return builder.toString();
    }

    private void flush(Stack<Character> stack, StringBuilder builder) {
      while (!stack.isEmpty()) {
        stack.pop();
        builder.append("</sup>");
      }
    }
  }
}
