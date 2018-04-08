package me.saket.dank.utils.markdown;

import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;

import javax.inject.Inject;

public class AndroidBasedHtmlToSpansParser implements HtmlToSpansParser {

  private final DankHtmlTagHandler htmlTagHandler;

  @Inject
  public AndroidBasedHtmlToSpansParser(DankHtmlTagHandler htmlTagHandler) {
    this.htmlTagHandler = htmlTagHandler;
  }

  @Override
  public CharSequence parse(String html) {
    try {
      String sourceWithCustomTags = htmlTagHandler.overrideTags(html);
      Spannable spannable = (Spannable) Html.fromHtml(sourceWithCustomTags, null, htmlTagHandler);
      spannable = htmlTagHandler.replaceBlockQuoteSpans(spannable);
      return trimTrailingWhitespace(spannable);

    } catch (Exception e) {
      e.printStackTrace();
      return Html.fromHtml(html);
    }
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public String stripMarkdownFromHtml(String html) {
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
}
