package me.saket.dank.utils.glide;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.LibraryGlideModule;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import me.saket.dank.di.DankAppModule;
import me.saket.dank.utils.okhttp.OkHttpResponseReadProgressListener;
import me.saket.dank.utils.okhttp.OkHttpResponseBodyWithProgress;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@GlideModule
public class GlideOkHttpProgressModule extends LibraryGlideModule {

  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .connectTimeout(DankAppModule.NETWORK_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(DankAppModule.NETWORK_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addNetworkInterceptor(createInterceptor(new OkHttpProgressListenersRepository()))
        .build();

    registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(okHttpClient));
  }

  private static Interceptor createInterceptor(final OkHttpResponseReadProgressListener listener) {
    return chain -> {
      Request request = chain.request();
      Response response = chain.proceed(request);
      return response.newBuilder()
          .body(new OkHttpResponseBodyWithProgress(request.url(), response.body(), listener))
          .build();
    };
  }

  public interface UiProgressListener {
    void onProgress(long bytesRead, long expectedLength);

    /**
     * Control how often the listener needs an update. 0% and 100% will always be dispatched.
     *
     * @return in percentage (0.2 = call {@link #onProgress} around every 0.2 percent of progress)
     */
    float getGranularityPercentage();
  }

  public static void forget(String url) {
    OkHttpProgressListenersRepository.removeUiProgressListener(url);
  }

  public static void expect(String url, UiProgressListener listener) {
    OkHttpProgressListenersRepository.addUiProgressListener(url, listener);
  }
}
