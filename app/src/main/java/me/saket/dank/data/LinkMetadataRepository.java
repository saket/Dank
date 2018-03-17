package me.saket.dank.data;

import android.support.annotation.CheckResult;

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
import me.saket.dank.utils.DiskLruCachePathResolver;
import me.saket.dank.utils.MoshiStoreJsonParser;
import me.saket.dank.utils.StoreFilePersister;
import me.saket.dank.utils.Urls;
import timber.log.Timber;

@Singleton
public class LinkMetadataRepository {

  private final Store<LinkMetadata, Link> linkMetadataStore;

  @Inject
  public LinkMetadataRepository(DankApi dankApi, FileSystem cacheFileSystem, Moshi moshi) {
    DiskLruCachePathResolver<Link> pathResolver = new DiskLruCachePathResolver<Link>() {
      @Override
      protected String resolveIn64Letters(Link key) {
        String url = key.unparsedUrl();
        return url.hashCode() + "_" + Urls.parseDomainName(url) + "_" + Urls.parseFileNameWithExtension(url);
      }
    };
    StoreFilePersister.JsonParser<LinkMetadata> jsonParser = new MoshiStoreJsonParser<>(moshi, LinkMetadata.class);

    linkMetadataStore = StoreBuilder.<Link, LinkMetadata>key()
        .fetcher(link -> unfurlLinkFromRemote(dankApi, link))
        .persister(new StoreFilePersister<>(cacheFileSystem, pathResolver, jsonParser))
        .open();
  }

  @CheckResult
  public Single<LinkMetadata> unfurl(Link link) {
    return linkMetadataStore.get(link).doOnError(e -> Timber.e(e, "Couldn't unfurl link: %s", link));
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
