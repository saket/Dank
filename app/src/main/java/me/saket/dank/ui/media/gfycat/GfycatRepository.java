package me.saket.dank.ui.media.gfycat;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Single;
import me.saket.dank.di.DankApi;
import me.saket.dank.urlparser.GfycatLink;
import me.saket.dank.urlparser.UrlParserConfig;
import retrofit2.HttpException;

import static java.lang.System.currentTimeMillis;

public class GfycatRepository {

  private final Lazy<DankApi> dankApi;
  private final Lazy<UrlParserConfig> urlParserConfig;
  private final Lazy<GfycatRepositoryData> data;

  @Inject
  public GfycatRepository(
      Lazy<DankApi> dankApi,
      Lazy<UrlParserConfig> urlParserConfig,
      Lazy<GfycatRepositoryData> data)
  {
    this.dankApi = dankApi;
    this.urlParserConfig = urlParserConfig;
    this.data = data;
  }

  public Single<GfycatLink> gif(String threeWordId) {
    return Single.fromCallable(() -> data.get().isAccessTokenRequired())
        .flatMap(headerRequired -> headerRequired
            ? authToken().flatMap(authHeader -> dankApi.get().gfycat(authHeader, threeWordId))
            : dankApi.get().gfycat(threeWordId))
        .retry(error -> {
          // At the time of writing this, Gfycat allows API calls without auth headers.
          // I'm going to wing it to reduce API calls until Gfycat finds out and makes
          // auth-header necessary.
          boolean authHeaderMissingError = error instanceof HttpException && ((HttpException) error).code() == 403;
          if (!data.get().isAccessTokenRequired() && authHeaderMissingError) {
            data.get().setAccessTokenRequired(true);
            return true;
          }
          return false;
        })
        .map(response -> {
          String unparsedUrl = urlParserConfig.get().gfycatUnparsedUrlPlaceholder(response.data().threeWordId());
          return GfycatLink.create(unparsedUrl, response.data().threeWordId(), response.data().highQualityUrl(), response.data().lowQualityUrl());
        });
  }

  private Single<String> authToken() {
    return data.get().tokenExpiryTimeMillis()
        .map(expiryTimeMillis -> {
          long fiveMinutesAgo = currentTimeMillis() - TimeUnit.MINUTES.toMillis(5);
          return expiryTimeMillis < fiveMinutesAgo;
        })
        .flatMapCompletable(hasTokenExpired -> {
          if (hasTokenExpired) {
            return dankApi.get()
                .gfycatOAuth("2_K1VUup", "vk8KwIPVFNa2eRWr7JbPfACeG0LPAVw2nHZ-cWc19te7RaMr0X_UrSKXOYHClctA")
                .flatMapCompletable(response -> data.get().saveOAuthResponse(response));
          } else {
            return Completable.complete();
          }
        })
        .andThen(data.get().accessToken());
  }
}
