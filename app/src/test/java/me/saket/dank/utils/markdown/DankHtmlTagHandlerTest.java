package me.saket.dank.utils.markdown;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class DankHtmlTagHandlerTest {

  private static final String HTML_WITH_LIST_INSIDE_QUOTE = "<!-- SC_OFF --><div class=\"md\"><h1>hello, This is Markdown Live Preview</h1> <hr/> "
      + "<hr/> <h2>what is Markdown?</h2> <p>see <a href=\"http://en.wikipedia.org/wiki/Markdown\">Wikipedia</a></p> <blockquote> <p>Markdown is a "
      + "lightweight markup language, originally created by John Gruber and Aaron Swartz allowing people &quot;to write using an easy-to-read, easy-to-"
      + "write plain text.</p> <blockquote> <p>Multiple angle brackets will result in nested quotes.</p> </blockquote> </blockquote> <hr/> <ol> <li>"
      + "Write markdown text in this textarea.</li> <li>Click &#39;HTML Preview&#39; button.</li> </ol> <hr/> <pre><code>Four or more leading spaces "
      + "will display as code, and will scroll rather than wrap. </code></pre> <p>The backtick key <code>can also be used</code> inline.</p> <p>Using "
      + "the caret sign <sup>will</sup> create exponentials You can use a series of these too - Y<sup>a<sup>a<sup>a<sup>a<sup>a<sup>a<sup>a</sup></sup>"
      + "</sup></sup></sup></sup></sup></p> <p><del>Two tildes on either end creates strike through</del></p> <table><thead> <tr> <th align=\"left\">"
      + "some</th> <th align=\"center\">header</th> <th align=\"right\">labels</th> </tr> </thead><tbody> <tr> <td align=\"left\">Left-justified</td> "
      + "<td align=\"center\">center-justified</td> <td align=\"right\">right-justified</td> </tr> <tr> <td align=\"left\">a</td> <td align=\"center\">"
      + "b</td> <td align=\"right\">c</td> </tr> <tr> <td align=\"left\">d</td> <td align=\"center\">e</td> <td align=\"right\">f</td> </tr> </tbody>"
      + "</table> <p>Ordered list inside quotes:</p> <blockquote> <p>Help! Mr. Kitty needs a new home!\\nJack the cat has been with our family for about"
      + " 10 years and is very sweet but unfortunately the portly fur ball is extremely high maintenance and I&#39;m not home enough to take care of "
      + "him to his satisfaction.</p> <p>His ideal home would have</p> <ol> <li><p>No other pets. He is a bully and other pets will make him act out."
      + "</p></li> <li><p>Ready access to outside! When it&#39;s not 90+ degrees you can leave him out all day! </p></li> <li><p>Someone who is home "
      + "frequently. He gets lonely and will let you know about it through bodily functions.</p></li> </ol> <p>If anyone has any ideas I&#39;d really"
      + " appreciate it. I would really prefer to keep him out of a shelter.</p> <p>Thanks!</p> </blockquote> </div><!-- SC_ON -->";

  private static final String HTML_WITH_NORMALIZED_LIST_INSIDE_QUOTE = "<!-- SC_OFF --><div class=\"md\"><h1>hello, This is Markdown Live Preview</h1>"
      + " <hr/> <hr/> <h2>what is Markdown?</h2> <p>see <a href=\"http://en.wikipedia.org/wiki/Markdown\">Wikipedia</a></p> <blockquote> <p>Markdown is"
      + " a lightweight markup language, originally created by John Gruber and Aaron Swartz allowing people &quot;to write using an easy-to-read, easy-"
      + "to-write plain text.</p> <blockquote> <p>Multiple angle brackets will result in nested quotes.</p> </blockquote> </blockquote> <hr/> <ol> <li>"
      + "Write markdown text in this textarea.</li> <li>Click &#39;HTML Preview&#39; button.</li> </ol> <hr/> <pre><code>Four or more leading spaces "
      + "will display as code, and will scroll rather than wrap. </code></pre> <p>The backtick key <code>can also be used</code> inline.</p> <p>Using "
      + "the caret sign <sup>will</sup> create exponentials You can use a series of these too - Y<sup>a<sup>a<sup>a<sup>a<sup>a<sup>a<sup>a</sup></sup>"
      + "</sup></sup></sup></sup></sup></p> <p><del>Two tildes on either end creates strike through</del></p> <table><thead> <tr> <th align=\"left\">"
      + "some</th> <th align=\"center\">header</th> <th align=\"right\">labels</th> </tr> </thead><tbody> <tr> <td align=\"left\">Left-justified</td> "
      + "<td align=\"center\">center-justified</td> <td align=\"right\">right-justified</td> </tr> <tr> <td align=\"left\">a</td> <td align=\"center\">"
      + "b</td> <td align=\"right\">c</td> </tr> <tr> <td align=\"left\">d</td> <td align=\"center\">e</td> <td align=\"right\">f</td> </tr> </tbody>"
      + "</table> <p>Ordered list inside quotes:</p> <blockquote> <p>Help! Mr. Kitty needs a new home!\\nJack the cat has been with our family for about"
      + " 10 years and is very sweet but unfortunately the portly fur ball is extremely high maintenance and I&#39;m not home enough to take care of"
      + " him to his satisfaction.</p> <p>His ideal home would have</p>  1. <p>No other pets. He is a bully and other pets will make him act out.</p>"
      + "<br/> 2. <p>Ready access to outside! When it&#39;s not 90+ degrees you can leave him out all day! </p><br/> 3. <p>Someone who is home"
      + " frequently. He gets lonely and will let you know about it through bodily functions.</p><br/>  <p>If anyone has any ideas I&#39;d really "
      + "appreciate it. I would really prefer to keep him out of a shelter.</p> <p>Thanks!</p> </blockquote> </div><!-- SC_ON -->";

  @Test
  public void normalizeListsInsideBlockQuotes() {
    String html = DankHtmlTagHandler.normalizeListsInsideBlockQuotes(HTML_WITH_LIST_INSIDE_QUOTE);
    assertEquals(HTML_WITH_NORMALIZED_LIST_INSIDE_QUOTE, html);
  }
}
