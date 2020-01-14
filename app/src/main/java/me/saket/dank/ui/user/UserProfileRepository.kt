package me.saket.dank.ui.user

import androidx.annotation.CheckResult
import com.nytimes.android.external.fs3.filesystem.FileSystem
import com.nytimes.android.external.store3.base.impl.MemoryPolicy
import com.nytimes.android.external.store3.base.impl.Store
import com.nytimes.android.external.store3.base.impl.StoreBuilder
import com.squareup.moshi.Moshi
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import me.saket.dank.cache.DiskLruCachePathResolver
import me.saket.dank.cache.MoshiStoreJsonParser
import me.saket.dank.cache.StoreFilePersister
import me.saket.dank.reddit.Reddit
import me.saket.dank.urlparser.RedditUserLink
import me.saket.dank.utils.Preconditions
import net.dean.jraw.models.Account
import net.dean.jraw.models.AccountStatus
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    private val reddit: Lazy<Reddit>,
    fileSystem: FileSystem,
    moshi: Moshi,
    private val userSessionRepository: UserSessionRepository
) {
  private val userProfileStore: Store<UserProfileSearchResult, String>
  private val loggedInUserAccountStore: Store<Account, String>

  init {
    userProfileStore = StoreBuilder.key<String, UserProfileSearchResult>()
        .fetcher { username -> userProfile(username) }
        .memoryPolicy(MemoryPolicy.builder()
            // FIXME: Memory-only store doesn't support expire-after-access policy.
            // FIXME: Expiry time isn't optional.
            .setExpireAfterWrite(6)
            .setExpireAfterTimeUnit(TimeUnit.HOURS)
            .setMemorySize(30)
            .build())
        .open()

    val pathResolver = object : DiskLruCachePathResolver<String>() {
      override fun resolveIn64Letters(username: String): String {
        return "logged_in_user_v2_$username"
      }
    }
    val parser = MoshiStoreJsonParser<Account>(moshi, Account::class.java)

    loggedInUserAccountStore = StoreBuilder.key<String, Account>()
        .fetcher { reddit.get().loggedInUser().about() }
        .memoryPolicy(MemoryPolicy.builder()
            .setExpireAfterWrite(6)
            .setExpireAfterTimeUnit(TimeUnit.HOURS)
            .build())
        .persister(StoreFilePersister(fileSystem, pathResolver, parser))
        .open()
  }

  private fun userProfile(username: String): Single<UserProfileSearchResult> {
    return reddit.get().users()
        .fetch(username)
        .map { query ->
          when (query.status) {
            AccountStatus.EXISTS -> UserProfile(query.account!!, userSubreddit = null)  // TODO JRAW: Get user's subreddit
            AccountStatus.NON_EXISTENT -> UserNotFound
            AccountStatus.SUSPENDED -> UserSuspended
            null -> throw AssertionError("Null status received when querying $username's profile")
          }
        }
  }

  @CheckResult
  fun profile(userLink: RedditUserLink): Single<UserProfileSearchResult> {
    val lowercaseName = userLink.name().toLowerCase(Locale.ENGLISH)
    return userProfileStore.get(lowercaseName)
        .onErrorReturn(::UnexpectedError)
  }

  @CheckResult
  fun loggedInUserAccounts(): Observable<Account> {
    val loggedInUserName = userSessionRepository.loggedInUserName()!!
    Preconditions.checkNotNull(loggedInUserName, "loggedInUserName == null")

    Timber.i("Fetching logged in user account")
    return loggedInUserAccountStore.getRefreshing(loggedInUserName)
  }

  @CheckResult
  fun refreshLoggedInUserAccount(): Completable {
    val loggedInUserName = userSessionRepository.loggedInUserName()!!

    Timber.i("Refreshing logged in user acct")
    return loggedInUserAccountStore.getWithResult(loggedInUserName)
        .filter { it.isFromCache }
        .flatMapSingle { loggedInUserAccountStore.fetch(loggedInUserName) }
        .toCompletable()
  }
}
