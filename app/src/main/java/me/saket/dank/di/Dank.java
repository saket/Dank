package me.saket.dank.di;

import android.app.Application;

import me.saket.dank.cache.CacheModule;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.notifs.MessagesNotificationManager;

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

  public static ErrorResolver errors() {
    return appComponent.errorManager();
  }

  public static MessagesNotificationManager messagesNotifManager() {
    return appComponent.messagesNotificationManager();
  }
}
