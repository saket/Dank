package me.saket.dank;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.os.StrictMode;

import com.facebook.stetho.Stetho;
import com.gabrielittner.threetenbp.LazyThreeTen;
import com.tspoon.traceur.Traceur;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.inflationx.viewpump.ViewPump;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.exceptions.OnErrorNotImplementedException;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.di.Dank;
import timber.log.Timber;

public class DankApplication extends Application {

  /**
   * Holds explicit references to observers that could get cleaned up by GC.
   */
  @SuppressWarnings("FieldCanBeLocal")
  private final CompositeDisposable referenceHolders = new CompositeDisposable();

  @Override
  public void onCreate() {
    super.onCreate();

    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());
      Stetho.initializeWithDefaults(this);
      Traceur.enableLogging();  // Throws an exception in every operator, so better enable only on debug builds

      StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
          .detectAll()
          .build());
      StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
          .detectLeakedSqlLiteObjects()
          .penaltyLog()
          .penaltyDeath()
          .build());
    }

    Dank.initDependencies(this);
    RxJavaPlugins.setErrorHandler(undeliveredExceptionsHandler());

    if (BuildConfig.DEBUG) {
      ViewPump.init(ViewPump.builder()
          .addInterceptor(Dank.dependencyInjector().typefaceInflationInterceptor())
          .build());
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      registerNotificationChannels();

      // Android doesn't print stack-traces on Oreo anymore if a custom exception handler is set.
      Thread.setDefaultUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler(Thread.getDefaultUncaughtExceptionHandler()));
    }

    Observable<Long> initialDelayStream = Observable.timer(5, TimeUnit.SECONDS, Schedulers.io())
        .replay()
        .refCount();

    referenceHolders.add(
        Dank.dependencyInjector().userAuthListener()
            .startListening(this)
            .subscribeOn(Schedulers.io())
            .startWith(initialDelayStream)
            .subscribe());

    LazyThreeTen.init(this);
    referenceHolders.add(
        initialDelayStream.subscribe(o -> LazyThreeTen.cacheZones()));

    referenceHolders.add(
        initialDelayStream
            .filter(o -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
            .map(o -> Dank.dependencyInjector().shortcutRepository())
            .flatMapCompletable(repository -> repository.updateInstalledShortcuts())
            .subscribe());
  }

  @TargetApi(Build.VERSION_CODES.O)
  private void registerNotificationChannels() {
    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    //noinspection ConstantConditions
    if (notificationManager.getNotificationChannel(getString(R.string.notification_channel_unread_messages_id)) != null) {
      // Channels already exist. Abort mission.
      return;
    }

    List<NotificationChannel> notifChannels = new ArrayList<>(3);

    // Unread messages.
    NotificationChannel privateMessagesChannel = new NotificationChannel(
        getString(R.string.notification_channel_unread_messages_id),
        getString(R.string.notification_channel_unread_messages),
        NotificationManager.IMPORTANCE_DEFAULT
    );
    privateMessagesChannel.setDescription(getString(R.string.notification_channel_unread_messages_description));
    privateMessagesChannel.enableLights(true);
    notifChannels.add(privateMessagesChannel);

    // Media downloads.
    NotificationChannel mediaDownloadsChannel = new NotificationChannel(
        getString(R.string.notification_channel_media_downloads_id),
        getString(R.string.notification_channel_media_downloads),
        NotificationManager.IMPORTANCE_DEFAULT
    );
    mediaDownloadsChannel.setDescription(getString(R.string.notification_channel_media_downloads_description));
    mediaDownloadsChannel.enableLights(false);
    notifChannels.add(mediaDownloadsChannel);

    // Debug notifs.
    if (BuildConfig.DEBUG) {
      NotificationChannel debugNotificationChannel = new NotificationChannel(
          getString(R.string.notification_channel_debug_notifs_id),
          "Debug notifications",
          NotificationManager.IMPORTANCE_DEFAULT
      );
      debugNotificationChannel.enableVibration(false);
      debugNotificationChannel.enableLights(false);
      notifChannels.add(debugNotificationChannel);
    }

    notificationManager.createNotificationChannels(notifChannels);
  }

  private Consumer<Throwable> undeliveredExceptionsHandler() {
    return e -> {
      e = Dank.errors().findActualCause(e);

      if ((e instanceof OnErrorNotImplementedException)) {
        Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
      }
      if (e instanceof IOException) {
        // Fine, file/network problem or API that throws on cancellation.
        Timber.w("IOException");
        e.printStackTrace();
        return;
      }
      if (e instanceof InterruptedException || e.getCause() instanceof InterruptedIOException) {
        // Fine, some blocking code was interrupted by a dispose call.
        Timber.w("Interrupted exception");
        return;
      }
      if ((e instanceof NullPointerException) || (e instanceof IllegalArgumentException)) {
        // That's likely a bug in the application.
        Timber.e(e, "Undeliverable exception");
        Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        return;
      }
      if (e instanceof IllegalStateException) {
        // That's a bug in RxJava or in a custom operator.
        Timber.e(e, "Undeliverable exception");
        Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        return;
      }

      Timber.e(e, "Undeliverable exception received, not sure what to do.");
    };
  }

  @TargetApi(Build.VERSION_CODES.O)
  static class LoggingUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Thread.UncaughtExceptionHandler defaultHandler;
    private boolean crashing;

    public LoggingUncaughtExceptionHandler(Thread.UncaughtExceptionHandler defaultHandler) {
      this.defaultHandler = defaultHandler;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable e) {
      try {
        // Don't re-enter -- avoid infinite loops if crash-reporting crashes.
        if (crashing) {
          return;
        }
        crashing = true;
        Timber.tag("Dank").e(e, "FATAL EXCEPTION: %s\nPID: %s", thread.getName(), Process.myPid());
      } finally {
        defaultHandler.uncaughtException(thread, e);
      }
    }
  }
}
