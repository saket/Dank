package me.saket.dank.di;

import com.danikula.videocache.HttpProxyCacheServer;

import javax.inject.Singleton;

import dagger.Component;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.SharedPrefsManager;
import okhttp3.OkHttpClient;

@Component(modules = DankAppModule.class)
@Singleton
public interface DankAppComponent {
    DankRedditClient dankRedditClient();

    SharedPrefsManager sharedPrefs();

    OkHttpClient okHttpClient();

    HttpProxyCacheServer httpProxyCacheServer();

    DankApi api();
}
