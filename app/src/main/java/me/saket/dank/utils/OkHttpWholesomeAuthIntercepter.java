package me.saket.dank.utils;

import java.io.IOException;

import me.saket.dank.di.DankApi;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;

public class OkHttpWholesomeAuthIntercepter implements Interceptor {

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request originalRequest = chain.request();

    if (originalRequest.url().host().contains(DankApi.WHOLESOME_API_HOST)) {
      Builder newRequestBuilder = originalRequest.newBuilder();
      newRequestBuilder.header(DankApi.HEADER_WHOLESOME_API_AUTH, Credentials.basic("triggered", "epochs-ramrod-diseuse-sioux"));
      return chain.proceed(newRequestBuilder.build());

    } else {
      return chain.proceed(originalRequest);
    }
  }
}
