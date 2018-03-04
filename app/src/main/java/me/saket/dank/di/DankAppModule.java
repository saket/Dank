package me.saket.dank.di;

import static me.saket.dank.utils.Units.dpToPx;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.danikula.videocache.HttpProxyCacheServer;
import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nytimes.android.external.cache3.Cache;
import com.nytimes.android.external.cache3.CacheBuilder;
import com.nytimes.android.external.fs3.filesystem.FileSystem;
import com.nytimes.android.external.fs3.filesystem.FileSystemFactory;
import com.squareup.moshi.Moshi;
import com.squareup.sqlbrite2.BriteDatabase;
import com.squareup.sqlbrite2.SqlBrite;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.http.LoggingMode;
import net.dean.jraw.http.UserAgent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.BuildConfig;
import me.saket.dank.R;
import me.saket.dank.data.CachePreFillThing;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.DankSqliteOpenHelper;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.NetworkStrategy;
import me.saket.dank.data.OnLoginRequireListener;
import me.saket.dank.data.VotingManager;
import me.saket.dank.data.links.Link;
import me.saket.dank.data.links.RedditUserLink;
import me.saket.dank.markdownhints.MarkdownHintOptions;
import me.saket.dank.markdownhints.MarkdownSpanPool;
import me.saket.dank.ui.UrlRouter;
import me.saket.dank.ui.authentication.LoginActivity;
import me.saket.dank.ui.submission.DraftStore;
import me.saket.dank.ui.submission.ReplyRepository;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.urlparser.UrlParser;
import me.saket.dank.utils.AutoValueMoshiAdapterFactory;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.JacksonHelper;
import me.saket.dank.utils.MoshiAccountAdapter;
import me.saket.dank.utils.MoshiMessageAdapter;
import me.saket.dank.utils.MoshiOptionalAdapterFactory;
import me.saket.dank.utils.MoshiSubmissionAdapter;
import me.saket.dank.utils.OkHttpWholesomeAuthIntercepter;
import me.saket.dank.utils.RxPreferencesEnumTypeAdapter;
import me.saket.dank.utils.TimeInterval;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import timber.log.Timber;

@Module
public class DankAppModule {

  public static final int NETWORK_CONNECT_TIMEOUT_SECONDS = 15;
  public static final int NETWORK_READ_TIMEOUT_SECONDS = 10;
  private Application appContext;

  public DankAppModule(Application appContext) {
    this.appContext = appContext;
  }

  @Provides
  Application provideAppContext() {
    return appContext;
  }

  @Provides
  UserAgent provideRedditUserAgent() {
    try {
      PackageInfo packageInfo = appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0);
      return UserAgent.of("android", appContext.getPackageName(), packageInfo.versionName, "saketme");

    } catch (PackageManager.NameNotFoundException e) {
      throw new IllegalStateException("Couldn't get app version name");
    }
  }

  @Provides
  @Singleton
  RedditClient provideRedditClient(UserAgent redditUserAgent) {
    RedditClient redditClient = new RedditClient(redditUserAgent);
    redditClient.setLoggingMode(LoggingMode.ALWAYS);
    redditClient.getHttpAdapter().setConnectTimeout(NETWORK_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    redditClient.getHttpAdapter().setReadTimeout(NETWORK_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    return redditClient;
  }

  // Already singleton.
  @Provides
  AuthenticationManager provideRedditAuthManager() {
    return AuthenticationManager.get();
  }

  @Provides
  @Named("deviceUuid")
  public UUID provideDeviceUuid(SharedPreferences sharedPrefs) {
    String key = "deviceUuid";
    if (!sharedPrefs.contains(key)) {
      sharedPrefs.edit()
          .putString(key, UUID.randomUUID().toString())
          .apply();
    }
    return UUID.fromString(sharedPrefs.getString(key, null));
  }

  @Provides
  @Singleton
  DankRedditClient provideDankRedditClient(RedditClient redditClient, AuthenticationManager authManager, UserSessionRepository userSessionRepository,
      @Named("deviceUuid") UUID deviceUuid)
  {
    return new DankRedditClient(appContext, redditClient, authManager, userSessionRepository, deviceUuid);
  }

  @Provides
  @Singleton
  SharedPreferences provideSharedPrefs() {
    return PreferenceManager.getDefaultSharedPreferences(appContext);
  }

  @Provides
  RxSharedPreferences provideRxSharedPrefs(SharedPreferences sharedPrefs) {
    return RxSharedPreferences.create(sharedPrefs);
  }

  @Provides
  @Singleton
  OkHttpClient provideOkHttpClient() {
    OkHttpClient.Builder builder = new OkHttpClient.Builder()
        .connectTimeout(NETWORK_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(NETWORK_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);

    if (BuildConfig.DEBUG) {
      HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Timber.tag("OkHttp").d(message));
      logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
      builder.addInterceptor(logging);
      builder.addNetworkInterceptor(new StethoInterceptor());
    }
    builder.addNetworkInterceptor(new OkHttpWholesomeAuthIntercepter());

    return builder.build();
  }

  @Provides
  @Singleton
  Moshi provideMoshi(JacksonHelper jacksonHelper) {
    return new Moshi.Builder()
        .add(AutoValueMoshiAdapterFactory.create())
        .add(new MoshiMessageAdapter(jacksonHelper))
        .add(new MoshiSubmissionAdapter(jacksonHelper))
        .add(new MoshiAccountAdapter(jacksonHelper))
        .add(new MoshiOptionalAdapterFactory())
        .build();
  }

  @Provides
  @Singleton
  Retrofit provideRetrofit(OkHttpClient okHttpClient, Moshi moshi) {
    return new Retrofit.Builder()
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .baseUrl("http://saket.me/" /* This isn't used anywhere, but this value is not nullable. */)
        .client(okHttpClient)
        .validateEagerly(true)
        .build();
  }

  @Provides
  @Singleton
  DankApi providesDankApi(Retrofit retrofit) {
    return retrofit.create(DankApi.class);
  }

  @Provides
  @Singleton
  JacksonHelper provideJacksonHelper() {
    return new JacksonHelper(new ObjectMapper());
  }

  /**
   * Used for caching videos.
   */
  @Provides
  @Singleton
  HttpProxyCacheServer provideHttpProxyCacheServer() {
    return new HttpProxyCacheServer(appContext);
  }

  @Provides
  @Singleton
  BriteDatabase provideBriteDatabase() {
    SqlBrite sqlBrite = new SqlBrite.Builder()
        .logger(message -> Timber.tag("Database").v(message))
        .build();

    BriteDatabase briteDatabase = sqlBrite.wrapDatabaseHelper(new DankSqliteOpenHelper(appContext), Schedulers.io());
    //briteDatabase.setLoggingEnabled(BuildConfig.DEBUG);
    briteDatabase.setLoggingEnabled(false);
    return briteDatabase;
  }

//  @Singleton
//  @Provides
//  MemoryPolicy provideCachingPolicy() {
//    return MemoryPolicy.builder()
//        .setExpireAfter(1)
//        .setExpireAfterTimeUnit(TimeUnit.DAYS)
//        .build();
//  }

  @Provides
  @Singleton
  FileSystem provideCacheFileSystem() {
    try {
      return FileSystemFactory.create(appContext.getCacheDir());
    } catch (IOException e) {
      throw new RuntimeException("Couldn't create FileSystemFactory. Cache dir: " + appContext.getCacheDir());
    }
  }

  @Provides
  ErrorResolver provideErrorManager() {
    return new ErrorResolver();
  }

  @Provides
  @Named("drafts_max_retain_days")
  int provideCommentDraftsMaxRetainDays() {
    return appContext.getResources().getInteger(R.integer.recycle_drafts_older_than_num_days);
  }

  @Provides
  @Named(ReplyRepository.SHARED_PREFS_NAME)
  SharedPreferences provideSharedPrefsForReplyDraftStore() {
    return appContext.getSharedPreferences(ReplyRepository.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
  }

  @Provides
  @Named(VotingManager.SHARED_PREFS_NAME)
  SharedPreferences provideSharedPrefsForVotingManager() {
    return appContext.getSharedPreferences(VotingManager.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
  }

  @Provides
  ConnectivityManager provideConnectivityManager() {
    return (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
  }

  @Provides
  @Singleton
  MarkdownSpanPool provideMarkdownSpanPool() {
    return new MarkdownSpanPool();
  }

  @Provides
  MarkdownHintOptions provideMarkdownHintOptions(Application appContext) {
    return MarkdownHintOptions.builder()
        .syntaxColor(Color.CYAN)
        .blockQuoteIndentationRuleColor(Color.CYAN)
        .linkUrlColor(Color.GRAY)
        .blockQuoteTextColor(Color.LTGRAY)
        .textBlockIndentationMargin(dpToPx(8, appContext))
        .blockQuoteVerticalRuleStrokeWidth(dpToPx(4, appContext))
        .linkUrlColor(Color.LTGRAY)
        .horizontalRuleColor(Color.LTGRAY)
        .horizontalRuleStrokeWidth(dpToPx(1.5f, appContext))
        .build();
  }

  @Provides
  @Singleton
  DankLinkMovementMethod provideBetterLinkMovementMethod(UrlRouter urlRouter, UrlParser urlParser) {
    DankLinkMovementMethod linkMovementMethod = DankLinkMovementMethod.newInstance();
    linkMovementMethod.setOnLinkClickListener((textView, url) -> {
      Link parsedLink = urlParser.parse(url);
      Point clickedUrlCoordinates = linkMovementMethod.getLastUrlClickCoordinates();

      if (parsedLink instanceof RedditUserLink) {
        urlRouter.forLink(((RedditUserLink) parsedLink))
            .expandFrom(clickedUrlCoordinates)
            .open(textView);

      } else {
        urlRouter.forLink(parsedLink)
            .expandFrom(clickedUrlCoordinates)
            .open(textView.getContext());
      }
      return true;
    });
    return linkMovementMethod;
  }

  @Provides
  DraftStore provideReplyDraftStore(ReplyRepository replyRepository) {
    return replyRepository;
  }

  @Provides
  OnLoginRequireListener provideOnLoginRequireListener(Application appContext) {
    return () -> {
      Intent loginIntent = LoginActivity.intent(appContext);
      loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      appContext.startActivity(loginIntent);
    };
  }

  @Provides
  BitmapPool provideBitmapPool() {
    // Not adding Glide to the dagger graph intentionally. Glide objects
    // should be created in Activity, Fragment and View contexts instead.
    return Glide.get(appContext).getBitmapPool();
  }

  @Provides
  @Named("comment_count_in_submission_list_byline")
  Preference<Boolean> showCommentCountInBylinePref(RxSharedPreferences rxPrefs) {
    return rxPrefs.getBoolean("comment_count_in_submission_list_byline", false);
  }

  @Provides
  @Named("show_nsfw_content")
  Preference<Boolean> showNsfwContentPref(RxSharedPreferences rxPrefs) {
    return rxPrefs.getBoolean("show_nsfw_content", false);
  }

  @Provides
  RxPreferencesEnumTypeAdapter<NetworkStrategy> networkStrategyRxPreferencesEnumTypeAdapter() {
    return new RxPreferencesEnumTypeAdapter<>(NetworkStrategy.class);
  }

  @Provides
  @Singleton
  @Named("high_resolution_media_network_strategy")
  Preference<NetworkStrategy> highResolutionMediaNetworkStrategyPref(
      RxSharedPreferences rxPrefs,
      RxPreferencesEnumTypeAdapter<NetworkStrategy> networkStrategyTypeAdapter)
  {
    return rxPrefs.getObject("high_resolution_media_network_strategy", NetworkStrategy.WIFI_ONLY, networkStrategyTypeAdapter);
  }

  @Provides
  @Named("auto_play_videos_network_strategy")
  Preference<NetworkStrategy> autoPlayVideosNetworkStrategyPref(
      RxSharedPreferences rxPrefs,
      RxPreferencesEnumTypeAdapter<NetworkStrategy> networkStrategyTypeAdapter)
  {
    return rxPrefs.getObject("auto_play_videos_network_strategy", NetworkStrategy.WIFI_ONLY, networkStrategyTypeAdapter);
  }

  @Provides
  @Named("open_links_in_external_browser")
  Preference<Boolean> openLinksInExternalBrowserPref(RxSharedPreferences rxPrefs) {
    return rxPrefs.getBoolean("open_links_in_external_browser", false);
  }

  @Provides
  @Named("cache_pre_filling_network_strategies")
  Map<CachePreFillThing, Preference<NetworkStrategy>> cachePreFillingNetworkStrategies(
      RxSharedPreferences rxPrefs,
      RxPreferencesEnumTypeAdapter<NetworkStrategy> networkStrategyTypeAdapter)
  {
    Map<CachePreFillThing, Preference<NetworkStrategy>> strategies = new HashMap<>();
    for (CachePreFillThing thing : CachePreFillThing.values()) {
      strategies.put(
          thing,
          rxPrefs.getObject("cache_pre_filling_network_strategy_for_" + thing.name(), NetworkStrategy.WIFI_ONLY, networkStrategyTypeAdapter));
    }
    return strategies;
  }

  @Provides
  @Named("cache_pre_filling")
  Scheduler cachePreFillingScheduler() {
    return Schedulers.from(Executors.newCachedThreadPool());
  }

  @Provides
  @Singleton
  @Named("markdown")
  Cache<String, CharSequence> provideMarkdownCache() {
    return CacheBuilder.newBuilder()
        .expireAfterAccess(15, TimeUnit.MINUTES)
        .build();
  }

  @Provides
  @Singleton
  @Named("url_parser")
  Cache<String, Link> provideUrlParserCache() {
    return CacheBuilder.newBuilder()
        .maximumSize(100)
        .build();
  }

  @Provides
  @Named("unread_messages")
  Preference<TimeInterval> unreadMessagesPollIntervalPref(RxSharedPreferences rxSharedPrefs, Moshi moshi) {
    Preference.Converter<TimeInterval> timeUnitPrefConverter = new TimeInterval.TimeUnitPrefConverter(moshi);
    TimeInterval defaultInterval = TimeInterval.create(30, TimeUnit.MINUTES);
    return rxSharedPrefs.getObject("unread_messages_poll_interval", defaultInterval, timeUnitPrefConverter);
  }
}
