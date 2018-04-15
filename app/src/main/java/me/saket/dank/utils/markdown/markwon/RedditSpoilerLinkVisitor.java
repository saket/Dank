package me.saket.dank.utils.markdown.markwon;

import android.text.Spanned;

import org.commonmark.node.Link;
import org.commonmark.node.Text;

import java.util.Locale;

import me.saket.dank.markdownhints.MarkdownHintOptions;
import ru.noties.markwon.SpannableBuilder;
import ru.noties.markwon.SpannableConfiguration;
import ru.noties.markwon.renderer.SpannableMarkdownVisitor;
import timber.log.Timber;

class RedditSpoilerLinkVisitor extends SpannableMarkdownVisitor {

  private final MarkdownHintOptions markdownOptions;
  private final SpannableBuilder builder;

  public RedditSpoilerLinkVisitor(SpannableConfiguration configuration, MarkdownHintOptions markdownOptions, SpannableBuilder builder) {
    super(configuration, builder);
    this.markdownOptions = markdownOptions;
    this.builder = builder;
  }

  public static boolean isValidSpoilerUrl(String url) {
    return url.length() <= 3 && url.endsWith("s");
  }

  @Override
  public void visit(Link link) {
    String spoilerContent = link.getTitle();
    if (spoilerContent != null && isValidSpoilerUrl(link.getDestination())) {
      try {
        Text linkText = (Text) link.getFirstChild();
        String spoilerLabel = linkText.getLiteral().toUpperCase(Locale.ENGLISH);

        String spoilerLabelAndContent = spoilerContent.isEmpty()
            ? spoilerLabel
            : spoilerLabel + " " + spoilerContent;
        linkText.setLiteral(spoilerLabelAndContent);

        // Some padding around the text looks good.
        String padding = "\u00a0\u00a0";

        int labelStart = builder.length();
        int labelEnd = labelStart + spoilerLabel.length() + padding.length();
        //noinspection UnnecessaryLocalVariable
        int contentStart = labelEnd;
        int contentEnd = labelStart + linkText.getLiteral().length() + padding.length() * 2;

        builder.append(padding);
        visitChildren(link);
        builder.append(padding);

        int spoilerBackgroundColor = markdownOptions.spoilerHiddenContentOverlayColor();

        SpoilerLabelSpan labelSpan = new SpoilerLabelSpan(spoilerBackgroundColor);
        setSpan(labelStart, labelEnd, labelSpan);

        SpoilerContentSpan contentSpan = new SpoilerContentSpan(spoilerBackgroundColor, spoilerContent);
        setSpan(contentStart, contentEnd, contentSpan);

        setSpan(labelStart, contentEnd, new SpoilerRevealClickListenerSpan(labelSpan, contentSpan));

      } catch (Exception e) {
        Timber.e(e, "Couldn't parse spoiler link: " + link);
        super.visit(link);
      }

    } else {
      super.visit(link);
    }
  }

  private void setSpan(int start, int end, Object span) {
    builder.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  }
}
