package me.saket.dank.utils;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class DiskLruCachePathResolverTest {

  /**
   * NYT-Store seems to have a bug where it calls PathResolver#resolve()
   * for resolved keys again. This method's output should be unchanged
   * if the input is its own output.
   */
  @Test
  public void resolve() throws Exception {
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
    assertEquals("no, no, no_ a vigilante is just a man lost in scramble for his o", path);
  }
}
