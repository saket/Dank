package me.saket.dank.di;

import com.danikula.videocache.HttpProxyCacheServer;

import javax.inject.Singleton;

import dagger.Component;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.SharedPrefsManager;
import me.saket.dank.data.SubredditSubscriptionManager;
import me.saket.dank.data.UserPrefsManager;
import me.saket.dank.utils.ImgurManager;
import okhttp3.OkHttpClient;

@Component(modules = DankAppModule.class)
@Singleton
public interface DankAppComponent {
    DankRedditClient dankRedditClient();

    SharedPrefsManager sharedPrefs();

    OkHttpClient okHttpClient();

    HttpProxyCacheServer httpProxyCacheServer();

    DankApi api();

    UserPrefsManager userPrefs();

    ImgurManager imgur();

    SubredditSubscriptionManager subredditSubscriptionManager();
}
