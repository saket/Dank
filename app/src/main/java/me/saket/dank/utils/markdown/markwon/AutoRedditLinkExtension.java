package me.saket.dank.utils.markdown.markwon;

import com.google.auto.value.AutoValue;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.parser.PostProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import javax.inject.Inject;

import me.saket.dank.urlparser.UrlParserConfig;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Strings;

/**
 * Extension for automatically turning "r/subreddit" and "u/user" URLs into links.
 * Inspired by {@link AutoRedditLinkExtension}.
 */
public class AutoRedditLinkExtension implements Parser.ParserExtension {

  private final UrlParserConfig urlParserConfig;

  @Inject
  public AutoRedditLinkExtension(UrlParserConfig urlParserConfig) {
    this.urlParserConfig = urlParserConfig;
  }

  @Override
  public void extend(Parser.Builder parserBuilder) {
    parserBuilder.postProcessor(new AutoRedditLinkPostProcessor(urlParserConfig));
  }

  static class AutoRedditLinkPostProcessor implements PostProcessor {
    private final UrlParserConfig urlParserConfig;

    public AutoRedditLinkPostProcessor(UrlParserConfig urlParserConfig) {
      this.urlParserConfig = urlParserConfig;
    }

    @Override
    public Node process(Node node) {
      AutoRedditLinkVisitor visitor = new AutoRedditLinkVisitor();
      node.accept(visitor);
      return node;
    }

    private void linkify(Text text) {
      String literal = text.getLiteral();
      Iterable<RedditLinkSpan> links = extractLinks(literal);

      Node lastNode = text;
      int last = 0;
      for (RedditLinkSpan link : links) {
        String linkText = literal.substring(link.beginIndex(), link.endIndex());
        if (link.beginIndex() != last) {
          lastNode = insertNode(new Text(literal.substring(last, link.beginIndex())), lastNode);
        }
        Text contentNode = new Text(linkText);
        Link linkNode = new Link(link.url(), null);
        linkNode.appendChild(contentNode);
        lastNode = insertNode(linkNode, lastNode);
        last = link.endIndex();
      }
      if (last != literal.length()) {
        insertNode(new Text(literal.substring(last)), lastNode);
      }
      text.unlink();
    }

    List<RedditLinkSpan> extractLinks(String literal) {
      Optional<List<RedditLinkSpan>> linkSpans = Optional.empty();

      Matcher subredditMatcher = urlParserConfig.autoLinkSubredditPattern().matcher(literal);
      while (subredditMatcher.find()) {
        int linkStartIndex = Strings.firstNonWhitespaceCharacterIndex(literal, subredditMatcher.start());
        int linkEndIndex = subredditMatcher.end();
        String subredditName = subredditMatcher.group(1);

        if (linkSpans.isEmpty()) {
          linkSpans = Optional.of(new ArrayList<>(4));
        }
        String url = String.format("https://reddit.com/r/%s", subredditName);
        linkSpans.get().add(RedditLinkSpan.create(url, linkStartIndex, linkEndIndex));
      }

      Matcher userMatcher = urlParserConfig.autoLinkUserPattern().matcher(literal);
      while (userMatcher.find()) {
        int linkStartIndex = Strings.firstNonWhitespaceCharacterIndex(literal, userMatcher.start());
        int linkEndIndex = userMatcher.end();
        String userName = userMatcher.group(1);

        if (linkSpans.isEmpty()) {
          linkSpans = Optional.of(new ArrayList<>(4));
        }
        String url = String.format("https://reddit.com/u/%s", userName);
        linkSpans.get().add(RedditLinkSpan.create(url, linkStartIndex, linkEndIndex));
      }

      return linkSpans.orElse(Collections.emptyList());
    }

    private static Node insertNode(Node node, Node insertAfterNode) {
      insertAfterNode.insertAfter(node);
      return node;
    }

    private class AutoRedditLinkVisitor extends AbstractVisitor {
      // This counter is used to ignore
      // visit() calls for the text of a link.
      int inLink = 0;

      @Override
      public void visit(Link link) {
        inLink++;
        super.visit(link);
        inLink--;
      }

      @Override
      public void visit(Text text) {
        if (inLink == 0) {
          try {
            linkify(text);
          } catch (Exception e) {
            //Timber.e("Couldn't auto-linkify reddit link: %s", text);
          }
        }
      }
    }
  }

  /**
   * Information for an extracted link.
   */
  @AutoValue
  public abstract static class RedditLinkSpan {

    abstract String url();

    /**
     * Begin index (inclusive) in the original input that this link starts at
     */
    abstract int beginIndex();

    /**
     * End index (exclusive) in the original input that this link ends at; in other words, index of first
     * character after link
     */
    abstract int endIndex();

    public static RedditLinkSpan create(String url, int beginIndex, int endIndex) {
      return new AutoValue_AutoRedditLinkExtension_RedditLinkSpan(url, beginIndex, endIndex);
    }
  }
}
