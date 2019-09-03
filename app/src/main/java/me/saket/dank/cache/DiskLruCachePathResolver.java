package me.saket.dank.cache;

import com.nytimes.android.external.fs3.PathResolver;
import com.nytimes.android.external.fs3.Util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Nonnull;

import io.reactivex.exceptions.Exceptions;

public abstract class DiskLruCachePathResolver<KEY> implements PathResolver<KEY> {

  private static final Util UTIL = new Util();

  @Nonnull
  @Override
  public String resolve(@Nonnull KEY key) {
    // FIXME: now that there's no limit of 64 letters, use composition instead.
    String path = resolveIn64Letters(key);
    return makeCompatibleWithDiskLruCache(path);
  }

  private String makeCompatibleWithDiskLruCache(String resolvedPath) {
    // Store library seems to clean paths before using them. This class does the same thing.
    String simplifiedPath = UTIL.simplifyPath(resolvedPath);
    if (simplifiedPath.startsWith("/")) {
      simplifiedPath = simplifiedPath.substring(1);
    }

    // DiskLruCache only allows: [a-z0-9_-]{1,64}
    return md5(simplifiedPath);
  }

  private static String md5(String string) {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      byte[] bytes = digest.digest(string.getBytes());
      BigInteger bigInt = new BigInteger(1, bytes);
      return bigInt.toString(16);

    } catch (NoSuchAlgorithmException e) {
      throw Exceptions.propagate(e);
    }
  }

  /**
   * Generate a key which will be unique even after it's truncated to 64 characters.
   */
  @SuppressWarnings("NullableProblems")
  protected abstract String resolveIn64Letters(KEY key);
}
