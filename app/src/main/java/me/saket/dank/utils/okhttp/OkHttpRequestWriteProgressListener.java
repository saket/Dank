package me.saket.dank.utils.okhttp;

/**
 * For listening to response read progress with {@link OkHttpRequestWriteProgressListener}.
 */
public interface OkHttpRequestWriteProgressListener {

  void update(long bytesRead, long totalBytes);
}
