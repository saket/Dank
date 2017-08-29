package me.saket.dank.utils.okhttp;

import android.support.annotation.NonNull;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * A response body that sends progress updates to a {@link OkHttpResponseReadProgressListener}.
 */
public class OkHttpResponseBodyWithProgress extends ResponseBody {

  private final HttpUrl url;
  private final ResponseBody responseBody;
  private final OkHttpResponseReadProgressListener progressListener;
  private BufferedSource bufferedSource;

  public static OkHttpResponseBodyWithProgress wrap(Request request, Response response, OkHttpResponseReadProgressListener progressListener) {
    return new OkHttpResponseBodyWithProgress(request.url(), response.body(), progressListener);
  }

  public OkHttpResponseBodyWithProgress(HttpUrl url, ResponseBody responseBody, OkHttpResponseReadProgressListener progressListener) {
    this.url = url;
    this.responseBody = responseBody;
    this.progressListener = progressListener;
  }

  @Override
  public MediaType contentType() {
    return responseBody.contentType();
  }

  @Override
  public long contentLength() {
    return responseBody.contentLength();
  }

  @Override
  public BufferedSource source() {
    if (bufferedSource == null) {
      bufferedSource = Okio.buffer(source(responseBody.source()));
    }
    return bufferedSource;
  }

  private Source source(Source source) {
    return new ForwardingSource(source) {
      long totalBytesRead = 0L;

      @Override
      public long read(@NonNull Buffer sink, long byteCount) throws IOException {
        long bytesRead = super.read(sink, byteCount);
        long fullLengthBytes = responseBody.contentLength();
        if (bytesRead == -1) { // this source is exhausted
          totalBytesRead = fullLengthBytes;
        } else {
          totalBytesRead += bytesRead;
        }
        progressListener.update(url, totalBytesRead, fullLengthBytes);
        return bytesRead;
      }
    };
  }
}
