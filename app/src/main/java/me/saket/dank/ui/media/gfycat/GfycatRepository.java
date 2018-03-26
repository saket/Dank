package me.saket.dank.ui.media.gfycat;

import javax.inject.Inject;

import dagger.Lazy;
import io.reactivex.Single;
import me.saket.dank.di.DankApi;
import me.saket.dank.urlparser.GfycatLink;
import me.saket.dank.urlparser.UrlParserConfig;

public class GfycatRepository {

  private final Lazy<DankApi> dankApi;
  private final Lazy<UrlParserConfig> urlParserConfig;

  @Inject
  public GfycatRepository(Lazy<DankApi> dankApi, Lazy<UrlParserConfig> urlParserConfig) {
    this.dankApi = dankApi;
    this.urlParserConfig = urlParserConfig;
  }

  public Single<GfycatLink> gif(String threeWordId) {
    return dankApi.get().gfycat(threeWordId)
        .map(response -> {
          String unparsedUrl = urlParserConfig.get().gfycatUnparsedUrlPlaceholder(response.data().threeWordId());
          return GfycatLink.create(unparsedUrl, response.data().highQualityUrl(), response.data().lowQualityUrl());
        });
  }
}
