package me.saket.dank.di;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.danikula.videocache.HttpProxyCacheServer;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.http.LoggingMode;
import net.dean.jraw.http.UserAgent;

import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.SharedPrefsManager;
import okhttp3.OkHttpClient;
import timber.log.Timber;

@Module
public class DankAppModule {

    private Context appContext;

    public DankAppModule(Application appContext) {
        this.appContext = appContext;
    }

    @Provides
    UserAgent provideRedditUserAgent() {
        try {
            PackageInfo packageInfo = appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0);
            return UserAgent.of("android", appContext.getPackageName(), packageInfo.versionName, "saketme");

        } catch (PackageManager.NameNotFoundException e) {
            Timber.e(e, "Couldn't get app version name");
            return null;
        }
    }

    @Provides
    AuthenticationManager provideRedditAuthManager() {
        return AuthenticationManager.get();
    }

    @Provides
    @Singleton
    DankRedditClient provideDankRedditClient(UserAgent redditUserAgent, AuthenticationManager authManager) {
        RedditClient redditClient = new RedditClient(redditUserAgent);
        redditClient.setLoggingMode(LoggingMode.ON_FAIL);
        return new DankRedditClient(appContext, redditClient, authManager);
    }

    @Provides
    @Singleton
    SharedPrefsManager provideSharedPrefsManager() {
        return new SharedPrefsManager(appContext);
    }

    @Provides
    @Singleton
    OkHttpClient provideOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton HttpProxyCacheServer provideHttpProxyCacheServer() {
        return new HttpProxyCacheServer(appContext);
    }

}
