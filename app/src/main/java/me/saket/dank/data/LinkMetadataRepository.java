package me.saket.dank.data;

import android.support.annotation.CheckResult;

import com.nytimes.android.external.fs3.PathResolver;
import com.nytimes.android.external.fs3.filesystem.FileSystem;
import com.nytimes.android.external.store3.base.impl.Store;
import com.nytimes.android.external.store3.base.impl.StoreBuilder;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import me.saket.dank.BuildConfig;
import me.saket.dank.data.links.Link;
import me.saket.dank.data.links.LinkMetadata;
import me.saket.dank.di.DankApi;
import me.saket.dank.utils.StoreFilePersister;
import me.saket.dank.utils.Urls;
import okio.BufferedSource;

@Singleton
public class LinkMetadataRepository {

  private final Store<LinkMetadata, Link> linkMetadataStore;

  @Inject
  public LinkMetadataRepository(DankApi dankApi, FileSystem cacheFileSystem, Moshi moshi) {
    PathResolver<Link> pathResolver = key -> {
      String url = key.unparsedUrl();
      return Urls.parseDomainName(url) + "_" + Urls.parseFileNameWithExtension(url) + "_" + url.hashCode();
    };
    StoreFilePersister.JsonParser<Link, LinkMetadata> jsonParser = new LinkMetadataStoreJsonParser(moshi);

    linkMetadataStore = StoreBuilder.<Link, LinkMetadata>key()
        .fetcher(link -> unfurlLinkFromRemote(dankApi, link))
        .persister(new StoreFilePersister<>(cacheFileSystem, pathResolver, jsonParser))
        .open();
  }

  @CheckResult
  public Single<LinkMetadata> unfurl(Link link) {
    return linkMetadataStore.get(link)
        // TODO: REMOVEEEEEE!
        .delay(BuildConfig.DEBUG ? 2 : 0, TimeUnit.SECONDS);
  }

  private static class LinkMetadataStoreJsonParser implements StoreFilePersister.JsonParser<Link, LinkMetadata> {
    private final JsonAdapter<LinkMetadata> jsonAdapter;

    public LinkMetadataStoreJsonParser(Moshi moshi) {
      this.jsonAdapter = moshi.adapter(LinkMetadata.class);
    }

    @Override
    public LinkMetadata fromJson(BufferedSource jsonBufferedSource) throws IOException {
      return jsonAdapter.fromJson(jsonBufferedSource);
    }

    @Override
    public String toJson(LinkMetadata value) {
      return jsonAdapter.toJson(value);
    }
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
