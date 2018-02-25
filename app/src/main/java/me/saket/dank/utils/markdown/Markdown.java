package me.saket.dank.utils.markdown;

import android.text.Html;
import android.text.Spanned;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.Submission;

import javax.inject.Inject;

import me.saket.dank.ui.submission.PendingSyncReply;
import me.saket.dank.utils.JrawUtils;

/**
 * Handles converting Reddit's markdown into Spans that can be rendered by TextView.
 * <p>
 * TODO: Proivide using Dagger.
 * TODO: Use MarkdownHints through this class.
 */
@SuppressWarnings({"StatementWithEmptyBody", "deprecation"})
public class Markdown {

  private final DankHtmlTagHandler htmlTagHandler;

  @Inject
  public Markdown(DankHtmlTagHandler htmlTagHandler) {
    this.htmlTagHandler = htmlTagHandler;
  }

  private CharSequence parse(String textWithMarkdown) {
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

  public CharSequence parse(Message message) {
    return parse(JrawUtils.messageBodyHtml(message));
  }

  public CharSequence parse(Comment comment) {
    return parse(JrawUtils.commentBodyHtml(comment));
  }

  public CharSequence parse(PendingSyncReply reply) {
    return parse(reply.body());
  }

  public CharSequence parseSelfText(Submission submission) {
    return parse(JrawUtils.selfPostHtml(submission));
  }

  /**
   * Reddit sends escaped body: "JRAW is escaping html entities.\n\n&lt; &gt; &amp;"
   * instead of "JRAW is escaping html entities.\n\n< > &".
   *
   * Convert "**Something**" -> "Something", without any styling.
   */
  public String stripMarkdown(String markdown) {
    // Since all styling is added using spans, converting the CharSequence to a String will remove all styling.
    String source = Html.fromHtml(markdown).toString();

    CharSequence result;
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
    int len = source.length();
    while (--len >= 0 && Character.isWhitespace(source.charAt(len))) {
    }
    return source.subSequence(0, len + 1);
  }
}
