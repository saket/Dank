package me.saket.dank.reddit.jraw

import android.app.Application
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import io.reactivex.subjects.BehaviorSubject
import me.saket.dank.BuildConfig
import me.saket.dank.R
import net.dean.jraw.RedditClient
import net.dean.jraw.android.AndroidHelper
import net.dean.jraw.android.AppInfo
import net.dean.jraw.android.AppInfoProvider
import net.dean.jraw.android.SharedPreferencesTokenStore
import net.dean.jraw.http.UserAgent
import net.dean.jraw.oauth.AccountHelper
import net.dean.jraw.oauth.TokenStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.util.*
import javax.inject.Named
import javax.inject.Singleton

@Module
class JrawRedditModule {

  @Provides
  @Singleton
  fun redditClientStreamSubject(): BehaviorSubject<RedditClient> {
    return BehaviorSubject.create()
  }

  @Provides
  fun appInfoProvider(appContext: Application): AppInfoProvider {
    return object : AppInfoProvider {
      override fun provide(): AppInfo {
        val version = appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
        val userAgent = UserAgent("android", appContext.packageName, version, appContext.getString(R.string.reddit_app_owner_username))

        return AppInfo(
            clientId = appContext.getString(R.string.reddit_app_client_id),
            redirectUrl = appContext.getString(R.string.reddit_app_redirect_url),
            userAgent = userAgent)
      }
    }
  }

  @Provides
  @Singleton
  fun sharedPrefsTokenStore(appContext: Application): SharedPreferencesTokenStore {
    val store = SharedPreferencesTokenStore(appContext)
    store.load()
    store.autoPersist = true
    return store
  }

  @Provides
  fun tokenStore(sharedPrefsTokenStore: SharedPreferencesTokenStore): TokenStore {
    return sharedPrefsTokenStore
  }

  @Provides
  @Named("deviceUuid")
  fun deviceUuid(sharedPrefs: SharedPreferences): UUID {
    val key = "deviceUuid"
    if (!sharedPrefs.contains(key)) {
      sharedPrefs.edit()
          .putString(key, UUID.randomUUID().toString())
          .apply()
    }
    return UUID.fromString(sharedPrefs.getString(key, null)!!)
  }

  @Provides
  @Singleton
  fun accountHelper(
      appInfoProvider: AppInfoProvider,
      tokenStore: SharedPreferencesTokenStore,
      tokenRefresher: JrawTokenRefresher,
      @Named("deviceUuid") deviceUUID: UUID
  ): AccountHelper {
    val httpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(tokenRefresher)
        .apply {
          if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor(
              object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                  Timber.tag("OkHttp").d(message)
                }
              }
            )
            logging.level = HttpLoggingInterceptor.Level.BASIC
            addInterceptor(logging)
          }
        }
        .build()
    return AndroidHelper.accountHelper(appInfoProvider, deviceUUID, tokenStore, httpClient)
  }
}
