package me.saket.dank.utils.markdown;

interface HtmlToSpansParser {

  CharSequence parse(String html);

  /**
   * Reddit sends escaped body: "JRAW is escaping html entities.\n\n&lt; &gt; &amp;"
   * instead of "JRAW is escaping html entities.\n\n< > &".
   * <p>
   * Convert "**Something**" -> "Something", without any styling.
   */
  String stripMarkdownFromHtml(String html);

  default CharSequence trimTrailingWhitespace(CharSequence source) {
    int len = source.length();
    //noinspection StatementWithEmptyBody
    while (--len >= 0 && Character.isWhitespace(source.charAt(len))) {
    }
    return source.subSequence(0, len + 1);
  }
}
