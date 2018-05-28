package me.saket.dank.reddit.jraw

import android.app.Application
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import me.saket.dank.R
import net.dean.jraw.android.AndroidHelper
import net.dean.jraw.android.AppInfo
import net.dean.jraw.android.AppInfoProvider
import net.dean.jraw.android.SharedPreferencesTokenStore
import net.dean.jraw.http.NetworkAdapter
import net.dean.jraw.http.OkHttpNetworkAdapter
import net.dean.jraw.http.UserAgent
import net.dean.jraw.oauth.AccountHelper
import net.dean.jraw.oauth.TokenStore
import java.util.UUID
import javax.inject.Named

@Module
class JrawRedditModule {

  @Provides
  fun networkAdapter(userAgent: UserAgent): NetworkAdapter {
    return OkHttpNetworkAdapter(userAgent)
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
  fun accountHelper(appInfoProvider: AppInfoProvider, @Named("deviceUuid") deviceUUID: UUID, tokenStore: TokenStore): AccountHelper {
    return AndroidHelper.accountHelper(appInfoProvider, deviceUUID, tokenStore)
  }
}
