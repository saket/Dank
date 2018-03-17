package me.saket.dank.di;

import android.app.Application;

import com.squareup.moshi.Moshi;

import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.SubredditSubscriptionManager;
import me.saket.dank.notifs.MessagesNotificationManager;
import me.saket.dank.utils.JacksonHelper;
import retrofit2.http.HEAD;

public class Dank {
  private static DankAppComponent appComponent;

  public static void initDependencies(Application application) {
    appComponent = DaggerDankAppComponent.builder()
        .rootModule(new RootModule(application))
        .userPreferencesModule(new UserPreferencesModule())
        .build();
  }

  public static DankAppComponent dependencyInjector() {
    return appComponent;
  }

  public static DankRedditClient reddit() {
    return appComponent.dankRedditClient();
  }

  public static SubredditSubscriptionManager subscriptions() {
    return appComponent.subredditSubscriptionManager();
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
