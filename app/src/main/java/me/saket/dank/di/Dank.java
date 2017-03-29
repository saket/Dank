package me.saket.dank.di;

import android.app.Application;

import com.danikula.videocache.HttpProxyCacheServer;

import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.SharedPrefsManager;
import okhttp3.OkHttpClient;

public class Dank {

    private static DankAppComponent appComponent;

    public static void initDependencies(Application application) {
        appComponent = DaggerDankAppComponent.builder()
                .dankAppModule(new DankAppModule(application))
                .build();
    }

    public static DankRedditClient reddit() {
        return appComponent.dankRedditClient();
    }

    public static SharedPrefsManager sharedPrefs() {
        return appComponent.sharedPrefs();
    }

    public static OkHttpClient okHttpClient() {
        return appComponent.okHttpClient();
    }

    public static HttpProxyCacheServer httpProxyCacheServer() {
        return appComponent.httpProxyCacheServer();
    }

}
