package me.saket.dank.data;

import android.support.annotation.CheckResult;

import com.nytimes.android.external.fs3.PathResolver;
import com.nytimes.android.external.fs3.filesystem.FileSystem;
import com.nytimes.android.external.store3.base.impl.Store;
import com.nytimes.android.external.store3.base.impl.StoreBuilder;
import com.squareup.moshi.Moshi;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Single;
import me.saket.dank.BuildConfig;
import me.saket.dank.data.links.Link;
import me.saket.dank.data.links.LinkMetadata;
import me.saket.dank.di.DankApi;
import me.saket.dank.utils.MoshiStoreJsonParser;
import me.saket.dank.utils.StoreFilePersister;
import me.saket.dank.utils.Urls;

@Singleton
public class LinkMetadataRepository {

  private final Store<LinkMetadata, Link> linkMetadataStore;

  @Inject
  public LinkMetadataRepository(DankApi dankApi, FileSystem cacheFileSystem, Moshi moshi) {
    PathResolver<Link> pathResolver = key -> {
      String url = key.unparsedUrl();
      return Urls.parseDomainName(url) + "_" + Urls.parseFileNameWithExtension(url) + "_" + url.hashCode();
    };
    StoreFilePersister.JsonParser<LinkMetadata> jsonParser = new MoshiStoreJsonParser<>(moshi, LinkMetadata.class);

    linkMetadataStore = StoreBuilder.<Link, LinkMetadata>key()
        .fetcher(link -> unfurlLinkFromRemote(dankApi, link))
        .persister(new StoreFilePersister<>(cacheFileSystem, pathResolver, jsonParser))
        .open();
  }

  @CheckResult
  public Single<LinkMetadata> unfurl(Link link) {
    return linkMetadataStore.get(link);
  }

  @CheckResult
  public Completable clearAll() {
    if (!BuildConfig.DEBUG) {
      throw new IllegalStateException();
    }
    return Completable.fromAction(() -> linkMetadataStore.clear());
  }

  private Single<LinkMetadata> unfurlLinkFromRemote(DankApi dankApi, Link link) {
    // Reddit uses different title for sharing to social media, which we don't want.
    boolean ignoreSocialMetadata = link.isRedditPage();
    return dankApi.unfurlUrl(link.unparsedUrl(), ignoreSocialMetadata)
        .map(response -> {
          if (response.error() == null) {
            //noinspection ConstantConditions
            return response.data().linkMetadata();
          } else {
            //noinspection ConstantConditions
            throw new RuntimeException(response.error().message());
          }
        });
  }
}
