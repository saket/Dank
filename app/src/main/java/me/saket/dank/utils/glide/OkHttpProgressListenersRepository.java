package me.saket.dank.utils.glide;

import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;

import me.saket.dank.utils.okhttp.OkHttpResponseReadProgressListener;
import okhttp3.HttpUrl;

/**
 * Holds all listeners attached to OkHttp response bodies and dispatches progress callbacks to them depending upon
 * {@link GlideOkHttpProgressModule.UiProgressListener#getGranularityPercentage()}.
 * <p>
 * We could have created a custom OkHttp client with a custom intercepter for each request to have local listeners,
 * but this also works where listeners are held globally.
 */
public class OkHttpProgressListenersRepository implements OkHttpResponseReadProgressListener {

  // Key for both the maps: URL.
  private static final Map<String, GlideOkHttpProgressModule.UiProgressListener> LISTENERS = new HashMap<>();
  private static final Map<String, Long> PROGRESSES = new HashMap<>();

  private final Handler handler;

  public OkHttpProgressListenersRepository() {
    this.handler = new Handler(Looper.getMainLooper());
  }

  public static void removeUiProgressListener(String url) {
    LISTENERS.remove(url);
    PROGRESSES.remove(url);
  }

  public static void addUiProgressListener(String url, GlideOkHttpProgressModule.UiProgressListener listener) {
    LISTENERS.put(url, listener);
  }

  @Override
  public void update(HttpUrl url, final long bytesRead, final long expectedContentLength) {
    //System.out.printf("%s: %d/%d = %.2f%%%n", url, bytesRead, contentLength, (100f * bytesRead) / contentLength);
    String key = url.toString();
    final GlideOkHttpProgressModule.UiProgressListener listener = LISTENERS.get(key);
    if (listener == null) {
      return;
    }
    if (expectedContentLength <= bytesRead) {
      removeUiProgressListener(key);
    }
    if (needsDispatch(key, bytesRead, expectedContentLength, listener.getGranularityPercentage())) {
      handler.post(() -> listener.onProgress(bytesRead, expectedContentLength));
    }
  }

  private boolean needsDispatch(String key, long current, long total, float granularity) {
    if (granularity == 0 || current == 0 || total == current) {
      return true;
    }
    float percent = 100f * current / total;
    long currentProgress = (long) (percent / granularity);
    Long lastProgress = PROGRESSES.get(key);
    if (lastProgress == null || currentProgress != lastProgress) {
      PROGRESSES.put(key, currentProgress);
      return true;
    } else {
      return false;
    }
  }
}
