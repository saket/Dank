package me.saket.dank.di;

import android.app.Application;

import com.danikula.videocache.HttpProxyCacheServer;
import com.squareup.moshi.Moshi;

import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.SubredditSubscriptionManager;
import me.saket.dank.data.VotingManager;
import me.saket.dank.notifs.MessagesNotificationManager;
import me.saket.dank.ui.user.UserSession;
import me.saket.dank.utils.JacksonHelper;

public class Dank {
  private static DankAppComponent appComponent;

  public static void initDependencies(Application application) {
    appComponent = DaggerDankAppComponent.builder()
        .dankAppModule(new DankAppModule(application))
        .build();
  }

  public static DankAppComponent dependencyInjector() {
    return appComponent;
  }

  public static DankRedditClient reddit() {
    return appComponent.dankRedditClient();
  }

  public static HttpProxyCacheServer httpProxyCacheServer() {
    return appComponent.httpProxyCacheServer();
  }

  public static DankApi api() {
    return appComponent.api();
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

  public static VotingManager voting() {
    return appComponent.votingManager();
  }

  public static UserSession userSession() {
    return appComponent.userSession();
  }
}
