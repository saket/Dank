package me.saket.dank.data

import androidx.annotation.CheckResult
import com.nytimes.android.external.fs3.filesystem.FileSystem
import com.nytimes.android.external.store3.base.impl.MemoryPolicy
import com.nytimes.android.external.store3.base.impl.Store
import com.nytimes.android.external.store3.base.impl.StoreBuilder
import com.squareup.moshi.Moshi
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Single
import me.saket.dank.BuildConfig
import me.saket.dank.cache.DiskLruCachePathResolver
import me.saket.dank.cache.MoshiStoreJsonParser
import me.saket.dank.cache.StoreFilePersister
import me.saket.dank.di.DankApi
import me.saket.dank.urlparser.Link
import me.saket.dank.utils.Urls
import me.thanel.dawn.linkunfurler.LinkMetadata
import me.thanel.dawn.linkunfurler.LinkUnfurler
import retrofit2.HttpException
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkMetadataRepository @Inject constructor(
  dankApi: Lazy<DankApi>,
  cacheFileSystem: FileSystem,
  moshi: Moshi,
  private val errorResolver: Lazy<ErrorResolver>,
  private val linkUnfurler: LinkUnfurler
) {
  private val linkMetadataStore: Store<LinkMetadata, Link>

  @CheckResult
  fun unfurl(link: Link): Single<LinkMetadata> {
    return linkMetadataStore[link]
      .doOnError { error ->
        if (error is NoSuchElementException) {
          Timber.e("'MaybeSource is empty' for %s", link)

        } else if (error is HttpException && error.code() == 500) {
          Timber.e("Wholesome server returned 500 error")

        } else {
          val resolvedError = errorResolver.get().resolve(error)
          resolvedError.ifUnknown {
            Timber.e(error, "Couldn't unfurl link: %s", link)
          }
        }
      }
  }

  @CheckResult
  fun clearAll(): Completable {
    check(BuildConfig.DEBUG)
    return Completable.fromAction { linkMetadataStore.clear() }
  }

  private fun unfurlLinkFromRemoteOnDevice(link: Link): Single<LinkMetadata> {
    // Reddit uses different title for sharing to social media, which we don't want.
    val ignoreSocialMetadata = link.isRedditPage
    return linkUnfurler.unfurl(link.unparsedUrl(), ignoreSocialMetadata)
      .map { it.getOrThrow() }
  }

  @Deprecated("Replaced by on-device unfurling")
  private fun unfurlLinkFromRemote(
    dankApi: DankApi,
    link: Link
  ): Single<LinkMetadata> {
    // Reddit uses different title for sharing to social media, which we don't want.
    val ignoreSocialMetadata = link.isRedditPage
    return dankApi.unfurlUrl(link.unparsedUrl(), ignoreSocialMetadata)
      .map { response ->
        val error = response.error()
        if (error != null) {
          throw RuntimeException(error.message())
        }

        val linkMetadata = response.data()!!.linkMetadata()
        return@map LinkMetadata(
          linkMetadata.url(),
          linkMetadata.title(),
          linkMetadata.faviconUrl(),
          linkMetadata.imageUrl()
        )
      }
  }

  init {
    val pathResolver = object : DiskLruCachePathResolver<Link>() {
      override fun resolveIn64Letters(key: Link): String {
        val url = key.unparsedUrl()
        val domainName = Urls.parseDomainName(url)
        val fileNameWithExtension = Urls.parseFileNameWithExtension(url)
        return "${url.hashCode()}_${domainName}_$fileNameWithExtension"
      }
    }

    val jsonParser = MoshiStoreJsonParser(moshi, LinkMetadata::class.java)

    linkMetadataStore = StoreBuilder.key<Link, LinkMetadata>()
      .fetcher { unfurlLinkFromRemoteOnDevice(it) }
      .memoryPolicy(
        MemoryPolicy.builder()
          .setMemorySize(100)
          .setExpireAfterWrite(24)
          .setExpireAfterTimeUnit(TimeUnit.HOURS)
          .build()
      )
      .persister(StoreFilePersister(cacheFileSystem, pathResolver, jsonParser))
      .open()
  }
}
