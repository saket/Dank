package me.saket.dank.utils.markdown.markwon;

import androidx.annotation.VisibleForTesting;
import android.text.SpannableStringBuilder;

import com.nytimes.android.external.cache3.Cache;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.Submission;

import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.node.Visitor;
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
import me.saket.dank.markdownhints.MarkdownHintOptions;
import me.saket.dank.reply.PendingSyncReply;
import me.saket.dank.utils.Preconditions;
import me.saket.dank.utils.markdown.Markdown;
import ru.noties.markwon.SpannableBuilder;
import ru.noties.markwon.SpannableConfiguration;
import ru.noties.markwon.tasklist.TaskListExtension;
import timber.log.Timber;

public class MarkwonBasedMarkdownRenderer implements Markdown {

  private static final Pattern LINK_MARKDOWN_PATTERN = Pattern.compile("\\[([^\\]]*)\\]\\(([^)\"]*)\\)");
  private static final Pattern LINK_WITH_SPACE_MARKDOWN_PATTERN = Pattern.compile("\\[([^\\]]*)\\]\\s+\\(([^)\"]*)\\)");
  private static final Pattern HEADING_WITHOUT_SPACE_MARKDOWN_PATTERN = Pattern.compile("(#{1,6})\\s{0}((?:(?!\\\\n).)*)");
  private static final Pattern POTENTIALLY_INVALID_SPOILER_MARKDOWN_PATTERN = Pattern.compile("\\[([^\\]]*)\\]\\((.*?)\"+(.*?(?<!\\\\))\"+\\)");

  private final MarkdownHintOptions markdownOptions;
  private final Cache<String, CharSequence> cache;
  private final Parser parser;
  private final SpannableConfiguration configuration;

  @Inject
  public MarkwonBasedMarkdownRenderer(
      SpannableConfiguration configuration,
      AutoRedditLinkExtension autoRedditLinkExtension,
      EmptyListItemHandlerExtension emptyListItemHandlerExtension,
      MarkdownHintOptions markdownOptions,
      @Named("markwon_spans_renderer") Cache<String, CharSequence> cache)
  {
    this.markdownOptions = markdownOptions;
    this.cache = cache;
    this.configuration = configuration;

    this.parser = new Parser.Builder()
        .extensions(Arrays.asList(
            StrikethroughExtension.create(),
            TablesExtension.create(),
            TaskListExtension.create(),
            AutolinkExtension.create(),
            emptyListItemHandlerExtension,
            autoRedditLinkExtension
        ))
        .build();
  }

  // TODO: it's important to call these typo-fixing methods in correct order.
  // Write a test to ensure that. Ensure ordering and integration of all.
  private SpannableStringBuilder parseMarkdown(String markdown) {
    // Convert '&lgt;' to '<', etc.
    markdown = org.jsoup.parser.Parser.unescapeEntities(markdown, true);
    markdown = fixInvalidTables(markdown);
    markdown = fixInvalidHeadings(markdown);
    markdown = removeSpaceBetweenLinkLabelAndUrl(markdown);
    markdown = escapeSpacesInLinkUrls(markdown);
    markdown = fixInvalidSpoilers(markdown);

    // WARNING: this should be at the end.
    markdown = new SuperscriptMarkdownToHtml().convert(markdown);

    // It's better **not** to re-use the visitor between multiple calls.
    SpannableBuilder builder = new SpannableBuilder();
    Visitor visitor = new RedditSpoilerLinkVisitor(configuration, markdownOptions, builder);

    Node node = parser.parse(markdown);
    node.accept(visitor);
    return (SpannableStringBuilder) builder.text();
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
    try {
      Preconditions.checkNotNull(submission.getSelfText(), "Self text is null");
      return getOrParse(submission.getSelfText());
    } catch (Throwable e) {
      throw new RuntimeException("Couldn't parse self-text in " + submission.getPermalink(), e);
    }
  }

  private String stripMarkdown(String markdown) {
    SpannableStringBuilder markdownWithStyling = (SpannableStringBuilder) getOrParse(markdown);

    SpoilerContentSpan[] spans = markdownWithStyling.getSpans(0, markdownWithStyling.length(), SpoilerContentSpan.class);
    for (SpoilerContentSpan spoilerSpan : spans) {
      markdownWithStyling = markdownWithStyling.replace(
          markdownWithStyling.getSpanStart(spoilerSpan),
          markdownWithStyling.getSpanEnd(spoilerSpan),
          "");
    }

    return markdownWithStyling.toString();
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  @Deprecated
  public String stripMarkdown(Comment comment) {
    return stripMarkdown(comment.getBody());
  }

  /**
   * See {@link #stripMarkdown(Comment)}.
   */
  @Override
  public String stripMarkdown(Message message) {
    return stripMarkdown(message.getBody());
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
    try {
      markdown = markdown
          .replace(":--|", ":---|")
          .replace("|:--:|", "|:---:|")
          .replace("|:--", "|:---")
          .replace("|--:", "|---:")
          .replace("|-:", "|---:");
    } catch (Throwable e) {
      Timber.e(e, "Couldn't fix table syntax in: %s", markdown);
    }
    return markdown;
  }

  /**
   * Ensures a space between '#' and heading text.
   */
  @VisibleForTesting
  String fixInvalidHeadings(String markdown) {
    try {
      Matcher matcher = HEADING_WITHOUT_SPACE_MARKDOWN_PATTERN.matcher(markdown);
      while (matcher.find()) {
        String heading = matcher.group(0);
        String hashes = matcher.group(1);
        String content = matcher.group(2).trim();
        markdown = markdown.replace(heading, String.format("%s %s", hashes, content));
      }
    } catch (Throwable e) {
      Timber.e(e, "Couldn't fix invalid headers in: %s", markdown);
    }

    return markdown;
  }

  @VisibleForTesting
  String removeSpaceBetweenLinkLabelAndUrl(String markdown) {
    try {
      Matcher matcher = LINK_WITH_SPACE_MARKDOWN_PATTERN.matcher(markdown);

      while (matcher.find()) {
        String linkText = matcher.group(1);
        String linkUrl = matcher.group(2);

        markdown = markdown.substring(0, matcher.start())
            + String.format("[%s](%s)", linkText, linkUrl)
            + markdown.substring(matcher.end(), markdown.length());
        //markdown = markdown.replace(matcher.group(0), String.format("[%s](%s)", linkText, linkUrl));
      }
    } catch (Throwable e) {
      Timber.e(e, "Couldn't remove spaces between link and url in: %s", markdown);
    }

    return markdown;
  }

  @VisibleForTesting
  String escapeSpacesInLinkUrls(String markdown) {
    try {
      Matcher matcher = LINK_MARKDOWN_PATTERN.matcher(markdown);

      while (matcher.find()) {
        String linkText = matcher.group(1);
        String linkUrl = matcher.group(2).replaceAll("\\s", "%20");

        markdown = markdown.substring(0, matcher.start())
            + String.format("[%s](%s)", linkText, linkUrl)
            + markdown.substring(matcher.end(), markdown.length());
        //markdown = markdown.replace(matcher.group(0), String.format("[%s](%s)", linkText, linkUrl));
      }
    } catch (Throwable e) {
      Timber.e(e, "Couldn't escape spaces in link url in: %s", markdown);
    }

    return markdown;
  }

  @VisibleForTesting
  String fixInvalidSpoilers(String markdown) {
    try {
      Matcher matcher = POTENTIALLY_INVALID_SPOILER_MARKDOWN_PATTERN.matcher(markdown);

      while (matcher.find()) {
        String fullMatch = matcher.group(0);
        String spoilerLabel = matcher.group(1);
        String spoilerUrl = matcher.group(2).trim();
        String spoilerContent = matcher.group(3);

        if (!RedditSpoilerLinkVisitor.isValidSpoilerUrl(spoilerUrl)) {
          // This will only match "/s", "#s", "# s ", "/ s ", /s ", and similar variations.
          continue;
        }

        markdown = markdown.replace(fullMatch, String.format("[%s](/s \"%s\")", spoilerLabel, spoilerContent));
      }
    } catch (Throwable e) {
      Timber.e(e, "Couldn't fix invalid spoilers in: %s", markdown);
    }

    return markdown;
  }

  /**
   * commonmark-java does not recognize '^'. This replaces all '^' with {@code <sup>} tags.
   */
  @VisibleForTesting
  static class SuperscriptMarkdownToHtml {
    public String convert(String markdown) {
      try {
        Stack<Character> stack = new Stack<>();
        StringBuilder builder = new StringBuilder(markdown.length());

        for (int i = 0; i < markdown.length(); i++) {
          char c = markdown.charAt(i);
          char nextC = (i + 1) < markdown.length() ? markdown.charAt(i + 1) : Character.MIN_VALUE;

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

      } catch (Throwable e) {
        Timber.e(e, "Couldn't convert superscript markdown to html in: %s", markdown);
        return markdown;
      }
    }

    private void flush(Stack<Character> stack, StringBuilder builder) {
      while (!stack.isEmpty()) {
        stack.pop();
        builder.append("</sup>");
      }
    }
  }
}
