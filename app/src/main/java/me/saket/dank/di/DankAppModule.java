package me.saket.dank.di;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.danikula.videocache.HttpProxyCacheServer;
import com.ryanharter.auto.value.moshi.AutoValueMoshiAdapterFactory;
import com.squareup.moshi.Moshi;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.http.LoggingMode;
import net.dean.jraw.http.UserAgent;

import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import me.saket.dank.BuildConfig;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.DankSqliteOpenHelper;
import me.saket.dank.data.SharedPrefsManager;
import me.saket.dank.data.SubredditSubscriptionManager;
import me.saket.dank.data.UserPrefsManager;
import me.saket.dank.utils.ImgurManager;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import rx.schedulers.Schedulers;
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
    @Singleton
    RedditClient provideRedditClient(UserAgent redditUserAgent) {
        RedditClient redditClient = new RedditClient(redditUserAgent);
        redditClient.setLoggingMode(LoggingMode.ON_FAIL);
        return redditClient;
    }

    @Provides
    AuthenticationManager provideRedditAuthManager() {
        return AuthenticationManager.get();
    }

    @Provides
    @Singleton
    DankRedditClient provideDankRedditClient(RedditClient redditClient, AuthenticationManager authManager) {
        return new DankRedditClient(appContext, redditClient, authManager);
    }

    @Provides
    @Singleton
    SharedPrefsManager provideSharedPrefsManager() {
        return new SharedPrefsManager(appContext);
    }

    @Provides
    @Singleton
    UserPrefsManager provideUserPrefsManager(SharedPrefsManager sharedPrefsManager) {
        return new UserPrefsManager(sharedPrefsManager);
    }

    @Provides
    @Singleton
    OkHttpClient provideOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS);

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Timber.tag("OkHttp").d(message));
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addNetworkInterceptor(logging);
        }

        return builder.build();
    }

    @Provides
    @Singleton
    DankApi providesKiteApi(Retrofit retrofit) {
        return retrofit.create(DankApi.class);
    }

    @Provides
    @Singleton
    Moshi provideMoshi() {
        return new Moshi.Builder()
                .add(new AutoValueMoshiAdapterFactory())
                .build();
    }

    @Provides
    @Singleton
    Retrofit provideRetrofit(OkHttpClient okHttpClient, Moshi moshi) {
        return new Retrofit.Builder()
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .baseUrl("http://saket.me/" /* This isn't used anywhere, but this value is not nullable. */)
                .client(okHttpClient)
                .build();
    }

    @Provides
    @Singleton
    HttpProxyCacheServer provideHttpProxyCacheServer() {
        return new HttpProxyCacheServer(appContext);
    }

    @Provides
    ImgurManager provideImgurManager() {
        return new ImgurManager(appContext);
    }

    @Provides
    @Singleton
    BriteDatabase provideBriteDatabase() {
        SqlBrite sqlBrite = new SqlBrite.Builder()
                .logger(message -> Timber.tag("Database").v(message))
                .build();

        BriteDatabase briteDatabase = sqlBrite.wrapDatabaseHelper(new DankSqliteOpenHelper(appContext), Schedulers.io());
        briteDatabase.setLoggingEnabled(true);
        return briteDatabase;
    }

    @Provides
    @Singleton
    SubredditSubscriptionManager provideSubredditSubscriptionManager(BriteDatabase briteDatabase, DankRedditClient dankRedditClient) {
        return new SubredditSubscriptionManager(appContext, briteDatabase, dankRedditClient);
    }

}
