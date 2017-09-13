package me.saket.dank;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;

import com.facebook.stetho.Stetho;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.tspoon.traceur.Traceur;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Arrays;

import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import me.saket.dank.di.Dank;
import timber.log.Timber;

public class DankApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();

    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());
      Stetho.initializeWithDefaults(this);
      Traceur.enableLogging();  // Throws an exception in every operator, so better enable only on debug builds
    }

    AndroidThreeTen.init(this);
    Dank.initDependencies(this);
    RxJavaPlugins.setErrorHandler(createUndeliveredExceptionsHandler());

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      createNotificationChannels();
    }
  }

  @TargetApi(Build.VERSION_CODES.O)
  private void createNotificationChannels() {
    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    //noinspection ConstantConditions
    if (notificationManager.getNotificationChannel(getString(R.string.notification_channel_unread_messages_id)) != null) {
      // Channels already exist. Abort mission.
      return;
    }

    // Unread messages.
    NotificationChannel privateMessagesChannel = new NotificationChannel(
        getString(R.string.notification_channel_unread_messages_id),
        getString(R.string.notification_channel_unread_messages),
        NotificationManager.IMPORTANCE_DEFAULT
    );
    privateMessagesChannel.setDescription(getString(R.string.notification_channel_unread_messages_description));

    // Media downloads.
    NotificationChannel mediaDownloadsChannel = new NotificationChannel(
        getString(R.string.notification_channel_media_downloads_id),
        getString(R.string.notification_channel_media_downloads),
        NotificationManager.IMPORTANCE_DEFAULT
    );
    mediaDownloadsChannel.setDescription(getString(R.string.notification_channel_media_downloads_description));
    mediaDownloadsChannel.enableLights(false);

    notificationManager.createNotificationChannels(Arrays.asList(privateMessagesChannel, mediaDownloadsChannel));
  }

  @NonNull
  private Consumer<Throwable> createUndeliveredExceptionsHandler() {
    return e -> {
      e = Dank.errors().findActualCause(e);

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
        Timber.e(e, e.getMessage());
        Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        return;
      }
      if (e instanceof IllegalStateException) {
        // That's a bug in RxJava or in a custom operator.
        Timber.e(e, e.getMessage());
        Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        return;
      }

      Timber.e(e, "Undeliverable exception received, not sure what to do.");
    };
  }
}
