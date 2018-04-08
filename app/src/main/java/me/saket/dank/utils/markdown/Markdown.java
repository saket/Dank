package me.saket.dank.utils.markdown;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.Submission;

import me.saket.dank.ui.submission.PendingSyncReply;

/**
 * Handles converting Reddit's markdown into Spans that can be rendered by TextView.
 * <p>
 * TODO: Use MarkdownHints through this class.
 */
public interface Markdown {

  CharSequence parse(PendingSyncReply reply);

  CharSequence parseAuthorFlair(String flair);

  CharSequence parse(Message message);

  CharSequence parse(Comment comment);

  CharSequence parseSelfText(Submission submission);

  /**
   * Reddit sends escaped body: "JRAW is escaping html entities.\n\n&lt; &gt; &amp;"
   * instead of "JRAW is escaping html entities.\n\n< > &".
   * <p>
   * Convert "**Something**" -> "Something", without any styling.
   */
  String stripMarkdown(Comment comment);

  String stripMarkdown(Message message);

  void clearCache();
}
