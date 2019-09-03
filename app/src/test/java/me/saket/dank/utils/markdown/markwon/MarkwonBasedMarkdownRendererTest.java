package me.saket.dank.utils.markdown.markwon;

import com.nytimes.android.external.cache3.Cache;

import org.junit.Before;
import org.junit.Test;

import me.saket.dank.markdownhints.MarkdownHintOptions;
import ru.noties.markwon.SpannableConfiguration;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class MarkwonBasedMarkdownRendererTest {

  private MarkwonBasedMarkdownRenderer renderer;

  @Before
  public void setUp() {
    //noinspection unchecked
    renderer = new MarkwonBasedMarkdownRenderer(
        mock(SpannableConfiguration.class),
        mock(AutoRedditLinkExtension.class),
        mock(EmptyListItemHandlerExtension.class),
        mock(MarkdownHintOptions.class),
        mock(Cache.class));
  }

  @Test
  public void fixInvalidTables() {
    // TODO.
  }

  @Test
  public void escapeSpacesInLinkUrls() {
    String invalid = "see [Wikipedia](http://en.wikipedia.org/wiki/Markdown)\n\n[Spoiler](/s \"text inside quotes\") [Spoiler](/s \"\") " +
        "[Spoiler](/s)\n\n*****\n\nSub: /r/pics r/pics\n\nUser: /u/Saketme/ u/saketme/\n\n*****\n\n[^Send ^feedback](https://www.reddit.com" +
        "/message/compose/?to=poochi&amp;amp;subject=New bot feedback)\n\n[spoiler](/s \"the right thing :p\")\n\n[spoiler](#s \"I will be " +
        "hanged\")\n\nEdit: fixed grammar";

    String expected = "see [Wikipedia](http://en.wikipedia.org/wiki/Markdown)\n" +
        "\n" +
        "[Spoiler](/s \"text inside quotes\") [Spoiler](/s \"\") [Spoiler](/s)\n" +
        "\n" +
        "*****\n" +
        "\n" +
        "Sub: /r/pics r/pics\n" +
        "\n" +
        "User: /u/Saketme/ u/saketme/\n" +
        "\n" +
        "*****\n" +
        "\n" +
        "[^Send ^feedback](https://www.reddit.com/message/compose/?to=poochi&amp;amp;subject=New%20bot%20feedback)\n" +
        "\n[spoiler](/s \"the right thing :p\")\n\n[spoiler](#s \"I will be hanged\")\n\nEdit: fixed grammar";

    String parsed = renderer.escapeSpacesInLinkUrls(invalid);
    assertEquals(expected, parsed);
  }

  @Test
  public void fixInvalidHeadings() {
    String invalid = "#Heading 1\n\n##Heading 2\n\n### Heading 3\n\n#### Heading 4\n\n#####Heading 5\n\n######Heading 6\n\nSome normal text with a # in between.";
    String expected = "# Heading 1\n\n## Heading 2\n\n### Heading 3\n\n#### Heading 4\n\n##### Heading 5\n\n###### Heading 6\n\nSome normal text with a # in between.";

    String parsed = renderer.fixInvalidHeadings(invalid);
    assertEquals(expected, parsed);
  }

  @Test
  public void fixInvalidLinks() {
    String invalid = "[title] (url)";
    String expected = "[title](url)";

    String parsed = renderer.removeSpaceBetweenLinkLabelAndUrl(invalid);
    assertEquals(expected, parsed);
  }

  @Test
  public void fixInvalidSpoilers() {
    String invalid = "[spoiler](/s \"I will be hanged\")\n" +
        "[spoiler](# s\"I will be hanged\")\n" +
        "[spoiler](/s \"\"you will hang me.\"\")\n" +
        "[spoiler](/s \"\"\"you will hang me.\"\"\")";

    String expected = "[spoiler](/s \"I will be hanged\")\n" +
        "[spoiler](/s \"I will be hanged\")\n" +
        "[spoiler](/s \"you will hang me.\")\n" +
        "[spoiler](/s \"you will hang me.\")";

    String parsed = renderer.fixInvalidSpoilers(invalid);
    assertEquals(expected, parsed);
  }

  @Test
  public void avoidFixingValidSpoilers() {
    String valid = "[FAQ](http://np.reddit.com/r/autotldr/comments/31b9fm/faq_autotldr_bot/ \"Version 2.00, ~310541 tl;drs so far.\")\n\n" +
        "[Feedback](http://np.reddit.com/message/compose?to=%23autotldr \"PM's and comments are monitored, constructive feedback is welcome.\")";
    String parsed = renderer.fixInvalidSpoilers(valid);
    assertEquals(valid, parsed);
  }
}
