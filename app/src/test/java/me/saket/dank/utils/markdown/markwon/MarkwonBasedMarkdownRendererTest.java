package me.saket.dank.utils.markdown.markwon;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import com.nytimes.android.external.cache3.Cache;

import org.junit.Before;
import org.junit.Test;

import ru.noties.markwon.SpannableConfiguration;

public class MarkwonBasedMarkdownRendererTest {

  private MarkwonBasedMarkdownRenderer renderer;

  @Before
  public void setUp() {
    //noinspection unchecked
    renderer = new MarkwonBasedMarkdownRenderer(mock(SpannableConfiguration.class), mock(AutoRedditLinkExtension.class), mock(Cache.class));
  }

  @Test
  public void fixInvalidTables() {
    // TODO.
  }

  @Test
  public void escapeSpacesInLinkUrls() {
    String invalid = "[^Send ^feedback](https://www.reddit.com/message/compose/?to=poochi&amp;amp;subject=New bot feedback)\\n";
    String expected = "[^Send ^feedback](https://www.reddit.com/message/compose/?to=poochi&amp;amp;subject=New%20bot%20feedback)\\n";

    String parsed = renderer.escapeSpacesInLinkUrls(invalid);
    assertEquals(expected, parsed);
  }
}
