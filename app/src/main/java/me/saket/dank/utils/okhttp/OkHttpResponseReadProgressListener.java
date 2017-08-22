package me.saket.dank.utils.okhttp;

import okhttp3.HttpUrl;

/**
 * For listening to response read progress with {@link OkHttpResponseBodyWithProgress}.
 */
public interface OkHttpResponseReadProgressListener {

  /**
   * @param expectedContentLength Expected length reported by the server. This may or may not be exact.
   */
  void update(HttpUrl url, long bytesRead, long expectedContentLength);
}
