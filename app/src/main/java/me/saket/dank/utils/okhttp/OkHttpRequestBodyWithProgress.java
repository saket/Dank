package me.saket.dank.utils.okhttp;

import android.support.annotation.NonNull;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * A response body that sends progress updates to a TODO {@link OkHttpResponseReadProgressListener}.
 */
public class OkHttpRequestBodyWithProgress extends RequestBody {

  private final RequestBody requestBody;
  private final OkHttpResponseReadProgressListener progressListener;
//
//  public static OkHttpRequestBodyWithProgress wrap(Request request, Response response, OkHttpResponseReadProgressListener progressListener) {
//    return new OkHttpRequestBodyWithProgress(request.url(), response.body(), progressListener);
//  }

  public OkHttpRequestBodyWithProgress(RequestBody requestBody, @NonNull OkHttpResponseReadProgressListener progressListener) {
    this.requestBody = requestBody;
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
    requestBody.writeTo(Okio.buffer(sinkWithProgress(sink)));
  }

  private Sink sinkWithProgress(BufferedSink sink) {
    return new ForwardingSink(sink) {
      @Override
      public void write(@NonNull Buffer source, long byteCount) throws IOException {
        super.write(source, byteCount);
        // Listen to write progress.
      }
    };
  }

//  private Source source(Source source) {
//    return new ForwardingSource(source) {
//      long totalBytesRead = 0L;
//
//      @Override
//      public long read(@NonNull Buffer sink, long byteCount) throws IOException {
//        long bytesRead = super.read(sink, byteCount);
//        long fullLengthBytes = requestBody.contentLength();
//        if (bytesRead == -1) { // this source is exhausted
//          totalBytesRead = fullLengthBytes;
//        } else {
//          totalBytesRead += bytesRead;
//        }
//        progressListener.update(totalBytesRead, fullLengthBytes);
//        return bytesRead;
//      }
//    };
//  }
}
