package me.saket.dank.utils.okhttp;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * A response body that sends progress updates to a {@link OkHttpRequestWriteProgressListener}.
 */
public class OkHttpRequestBodyWithProgress extends RequestBody {

  private final RequestBody requestBody;
  private final OkHttpRequestWriteProgressListener progressListener;

  public static OkHttpRequestBodyWithProgress wrap(RequestBody delegate, OkHttpRequestWriteProgressListener progressListener) {
    return new OkHttpRequestBodyWithProgress(delegate, progressListener);
  }

  public OkHttpRequestBodyWithProgress(RequestBody delegate, OkHttpRequestWriteProgressListener progressListener) {
    this.requestBody = delegate;
    this.progressListener = progressListener;
  }

  @Override
  public MediaType contentType() {
    return requestBody.contentType();
  }

  @Override
  public long contentLength() throws IOException {
    return requestBody.contentLength();
  }

  @Override
  public void writeTo(BufferedSink sink) throws IOException {
    try (BufferedSink bufferedSink = Okio.buffer(sinkWithProgress(sink))) {
      requestBody.writeTo(bufferedSink);
    }
  }

  private Sink sinkWithProgress(BufferedSink sink) {
    return new ForwardingSink(sink) {
      public long totalBytesRead;

      @Override
      public void write(Buffer source, long byteCount) throws IOException {
        super.write(source, byteCount);

        long fullLengthBytes = contentLength();
        if (byteCount == -1) { // this source is exhausted
          totalBytesRead = fullLengthBytes;
        } else {
          totalBytesRead += byteCount;
        }
        progressListener.update(totalBytesRead, fullLengthBytes);
      }
    };
  }
}
