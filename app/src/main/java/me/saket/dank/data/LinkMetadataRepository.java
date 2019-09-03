package me.saket.dank.data;

import androidx.annotation.CheckResult;

import com.nytimes.android.external.fs3.filesystem.FileSystem;
import com.nytimes.android.external.store3.base.impl.MemoryPolicy;
import com.nytimes.android.external.store3.base.impl.Store;
import com.nytimes.android.external.store3.base.impl.StoreBuilder;
import com.squareup.moshi.Moshi;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Single;
import me.saket.dank.BuildConfig;
import me.saket.dank.cache.DiskLruCachePathResolver;
import me.saket.dank.cache.MoshiStoreJsonParser;
import me.saket.dank.cache.StoreFilePersister;
import me.saket.dank.di.DankApi;
import me.saket.dank.urlparser.Link;
import me.saket.dank.urlparser.LinkMetadata;
import me.saket.dank.utils.Urls;
import retrofit2.HttpException;
import timber.log.Timber;

@Singleton
public class LinkMetadataRepository {

  private final Store<LinkMetadata, Link> linkMetadataStore;
  private final Lazy<ErrorResolver> errorResolver;

  @Inject
  public LinkMetadataRepository(Lazy<DankApi> dankApi, FileSystem cacheFileSystem, Moshi moshi, Lazy<ErrorResolver> errorResolver) {
    this.errorResolver = errorResolver;

    DiskLruCachePathResolver<Link> pathResolver = new DiskLruCachePathResolver<Link>() {
      @Override
      protected String resolveIn64Letters(Link key) {
        String url = key.unparsedUrl();
        return url.hashCode() + "_" + Urls.parseDomainName(url) + "_" + Urls.parseFileNameWithExtension(url);
      }
    };
    StoreFilePersister.JsonParser<LinkMetadata> jsonParser = new MoshiStoreJsonParser<>(moshi, LinkMetadata.class);

    linkMetadataStore = StoreBuilder.<Link, LinkMetadata>key()
        .fetcher(link -> unfurlLinkFromRemote(dankApi.get(), link))
        .memoryPolicy(MemoryPolicy.builder()
            .setMemorySize(100)
            .setExpireAfterWrite(24)
            .setExpireAfterTimeUnit(TimeUnit.HOURS)
            .build())
        .persister(new StoreFilePersister<>(cacheFileSystem, pathResolver, jsonParser))
        .open();
  }

  @CheckResult
  public Single<LinkMetadata> unfurl(Link link) {
    return linkMetadataStore.get(link)
        .doOnError(e -> {
          if (e instanceof NoSuchElementException) {
            Timber.e("'MaybeSource is empty' for %s", link);
          } else if (e instanceof HttpException && ((HttpException) e).code() == 500) {
            Timber.e("Wholesome server returned 500 error");
          } else {
            ResolvedError resolvedError = errorResolver.get().resolve(e);
            resolvedError.ifUnknown(() -> Timber.e(e, "Couldn't unfurl link: %s", link));
          }
        });
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
