package me.saket.dank.utils.markdown;

import static junit.framework.Assert.assertEquals;

import android.app.Application;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import me.saket.dank.markdownhints.MarkdownHintOptions;
import me.saket.dank.utils.markdown.markwon.MarkwonBasedMarkdownSpansRenderer;

@RunWith(AndroidJUnit4.class)
public class MarkdownTest {

  private Markdown markdown;

  @Before
  public void setUp() {
    Application appContext = (Application) InstrumentationRegistry.getTargetContext().getApplicationContext();
    MarkdownHintOptions options = MarkdownModule.provideMarkdownHintOptions(appContext);
    MarkwonBasedMarkdownSpansRenderer renderer = new MarkwonBasedMarkdownSpansRenderer(MarkdownModule.spannableConfiguration(appContext, options));
    markdown = new Markdown(renderer, MarkdownModule.markdownCache());
  }

  /**
   * Fails :(.
   */
  @Test
  public void parseMarkdown() {
    String markdownText = "# hello, This is Markdown Live Preview\\n----\\n*****\\n## what is Markdown?\\nsee [Wikipedia](http://en.wikipedia.org/"
        + "wiki/Markdown)\\n\\n[Spoiler](/s \\\"text inside quotes\\\")\\n\\n[Spoiler](/s \\\"\\\")\\n\\n[Spoiler](/s)\\n\\n&gt; Markdown is a "
        + "lightweight markup language, originally created by John Gruber and Aaron Swartz allowing people \\\"to write using an easy-to-read, "
        + "easy-to-write plain text.\\n&gt; &gt;Multiple angle brackets will result in nested quotes.\\n\\n----\\n1. Write markdown text in this"
        + " textarea.\\n2. Click 'HTML Preview' button.\\n\\n----\\n\\n    Four or more leading spaces will display as code, and will scroll "
        + "rather than wrap.\\n\\nThe backtick key `can also be used` inline.\\n\\nUsing the caret sign ^will create exponentials\\nYou can use "
        + "a series of these too - Y^a^a^a^a^a^a^a\\n\\n~~Two tildes on either end creates strike through~~\\n\\nsome|header|labels\\n:---|:--:|-:"
        + "\\nLeft-justified|center-justified|right-justified\\na|b|c\\nd|e|f\\n\\nOrdered list inside quotes:\\n\\n&gt;Help! Mr. Kitty needs a "
        + "new home!\\\\nJack the cat has been with our family for about 10 years and is very sweet but unfortunately the portly fur ball is "
        + "extremely high maintenance and I'm not home enough to take care of him to his satisfaction.\\n\\n&gt;His ideal home would have\\n\\n"
        + "&gt;- No other pets. He is a bully and other pets will make him act out.\\n\\n&gt;- Ready access to outside! When it's not 90+ degrees"
        + " you can leave him out all day! \\n\\n&gt;- Someone who is home frequently. He gets lonely and will let you know about it through "
        + "bodily functions.\\n\\n&gt;If anyone has any ideas I'd really appreciate it. I would really prefer to keep him out of a shelter.\\n\\n"
        + "&gt;Thanks!";

    String expectedOutput = "hello, This is Markdown Live Preview\n" +
        "\n" +
        " \n" +
        "\n" +
        " \n" +
        "\n" +
        "what is Markdown?\n" +
        "\n" +
        "see Wikipedia\n" +
        "\n" +
        "Spoiler\n" +
        "\n" +
        "Spoiler\n" +
        "\n" +
        "Spoiler\n" +
        "\n" +
        "Markdown is a lightweight markup language, originally created by John Gruber and Aaron Swartz allowing people \"to write using an easy-to-read, easy-to-write plain text.\n" +
        "\n" +
        "Multiple angle brackets will result in nested quotes.\n" +
        "\n" +
        " \n" +
        "\n" +
        "Write markdown text in this textarea.\n" +
        "Click 'HTML Preview' button.\n" +
        "\n" +
        " \n" +
        "\n" +
        " \n" +
        "Four or more leading spaces will display as code, and will scroll rather than wrap.\n" +
        " \n" +
        "\n" +
        "The backtick key  can also be used  inline.\n" +
        "\n" +
        "Using the caret sign will create exponentials You can use a series of these too - Yaaaaaaa\n" +
        "\n" +
        "Two tildes on either end creates strike through\n" +
        "\n" +
        " \n" +
        " \n" +
        " \n" +
        " \n" +
        "\n" +
        "Ordered list inside quotes:\n" +
        "\n" +
        "Help! Mr. Kitty needs a new home!\\nJack the cat has been with our family for about 10 years and is very sweet but unfortunately the portly fur ball is extremely high maintenance and I'm not home enough to take care of him to his satisfaction.\n" +
        "\n" +
        "His ideal home would have\n" +
        "\n" +
        "No other pets. He is a bully and other pets will make him act out.\n" +
        "\n" +
        "Ready access to outside! When it's not 90+ degrees you can leave him out all day!\n" +
        "\n" +
        "Someone who is home frequently. He gets lonely and will let you know about it through bodily functions.\n" +
        "\n" +
        "If anyone has any ideas I'd really appreciate it. I would really prefer to keep him out of a shelter.\n" +
        "\n" +
        "Thanks!";

    String parsedMarkdown = markdown.parseMarkdown(markdownText).toString();
    assertEquals(expectedOutput, parsedMarkdown);
  }
}
