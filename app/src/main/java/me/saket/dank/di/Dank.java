package me.saket.dank.di;

import android.app.Application;

import com.danikula.videocache.HttpProxyCacheServer;

import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.DataStores;
import me.saket.dank.data.ErrorManager;
import me.saket.dank.data.SharedPrefsManager;
import me.saket.dank.data.SubredditSubscriptionManager;
import me.saket.dank.data.UserPrefsManager;
import me.saket.dank.notifs.MessagesNotificationManager;
import me.saket.dank.utils.ImgurManager;
import me.saket.dank.utils.JacksonHelper;
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

    public static UserPrefsManager userPrefs() {
        return appComponent.userPrefs();
    }

    public static OkHttpClient okHttpClient() {
        return appComponent.okHttpClient();
    }

    public static HttpProxyCacheServer httpProxyCacheServer() {
        return appComponent.httpProxyCacheServer();
    }

    public static DankApi api() {
        return appComponent.api();
    }

    public static ImgurManager imgur() {
        return appComponent.imgur();
    }

    public static SubredditSubscriptionManager subscriptionManager() {
        return appComponent.subredditSubscriptionManager();
    }

    public static JacksonHelper jackson() {
        return appComponent.jacksonHelper();
    }

    public static DataStores stores() {
        return appComponent.dataStores();
    }

    public static ErrorManager errors() {
        return appComponent.errorManager();
    }

    public static MessagesNotificationManager messagesNotifManager() {
        return appComponent.messagesNotificationManager();
    }
}
