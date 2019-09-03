package me.saket.dank.utils;

import org.junit.Test;

import me.saket.dank.cache.DiskLruCachePathResolver;
import me.saket.dank.urlparser.Link;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class DiskLruCachePathResolverTest {

  /**
   * NYT-Store seems to have a bug where it calls PathResolver#resolve()
   * for resolved keys again. This method's output should be unchanged
   * if the input is its own output.
   */
  @Test
  public void testDeterministic() {
    DiskLruCachePathResolver<String> pathResolver = new DiskLruCachePathResolver<String>() {
      @Override
      protected String resolveIn64Letters(String o) {
        //noinspection SpellCheckingInspection
        return "AutoValue_StreamableUnresolvedLink_w3lfb";
      }
    };

    //noinspection ConstantConditions
    String resolved = pathResolver.resolve(null);
    String resolvedAgain = pathResolver.resolve(resolved);
    assertEquals(resolvedAgain, resolved);
  }

  @Test
  public void testConversionOfInvalidCharacters() {
    DiskLruCachePathResolver<Link> pathResolver = new DiskLruCachePathResolver<Link>() {
      @Override
      protected String resolveIn64Letters(Link key) {
        return "wikipedia.org/wiki/The_Lord_Üof_the_Rings_(film_series)";
      }
    };

    String resolvedPath = pathResolver.resolve(mock(Link.class));
    assertTrue(resolvedPath.matches("[a-z0-9_-]{1,64}"));
  }

  @Test
  public void testLength() {
    DiskLruCachePathResolver<String> pathResolver = new DiskLruCachePathResolver<String>() {
      @Override
      protected String resolveIn64Letters(String o) {
        //noinspection SpellCheckingInspection
        return "No, no, no. A vigilante is just a man lost in scramble for his own gratification. He can be destroyed or locked up. " +
            "But if you make yourself more than just a man, if you devote yourself to an idel and if they can't stop you then you become" +
            " something else entirely. Legend, Mr Wayne.";
      }
    };

    //noinspection ConstantConditions
    String path = pathResolver.resolve(null);
    assertEquals(true, path.length() <= 64);
    assertEquals("248f29bdc98078c71c3d38c537817e13", path);
  }
}
