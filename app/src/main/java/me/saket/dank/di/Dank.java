package me.saket.dank.di;

import android.app.Application;

import com.squareup.moshi.Moshi;

import me.saket.dank.cache.CacheModule;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.notifs.MessagesNotificationManager;
import me.saket.dank.ui.subscriptions.SubscriptionRepository;
import me.saket.dank.utils.JacksonHelper;

public class Dank {
  private static RootComponent appComponent;

  public static void initDependencies(Application application) {
    appComponent = DaggerRootComponent.builder()
        .rootModule(new RootModule(application))
        .userPreferencesModule(new UserPreferencesModule())
        .cacheModule(new CacheModule())
        .build();
  }

  public static RootComponent dependencyInjector() {
    return appComponent;
  }

  public static DankRedditClient reddit() {
    return appComponent.dankRedditClient();
  }

  public static SubscriptionRepository subscriptions() {
    return appComponent.subredditSubscriptionRepository();
  }

  public static JacksonHelper jackson() {
    return appComponent.jacksonHelper();
  }

  public static ErrorResolver errors() {
    return appComponent.errorManager();
  }

  public static MessagesNotificationManager messagesNotifManager() {
    return appComponent.messagesNotificationManager();
  }

  public static Moshi moshi() {
    return appComponent.moshi();
  }
}
