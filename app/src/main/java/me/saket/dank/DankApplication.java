package me.saket.dank;

import android.app.Activity;
import android.app.Application;
import android.support.annotation.CheckResult;

import com.facebook.stetho.Stetho;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;
import com.tspoon.traceur.Traceur;

import java.io.IOException;
import java.io.InterruptedIOException;

import io.reactivex.plugins.RxJavaPlugins;
import me.saket.dank.di.Dank;
import me.saket.dank.utils.SimpleActivityLifecycleCallbacks;
import timber.log.Timber;

public class DankApplication extends Application {

  private static Relay<Object> dankMinimizeRelay = PublishRelay.create();

  @Override
  public void onCreate() {
    super.onCreate();

    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());
      Stetho.initializeWithDefaults(this);
    }

    Dank.initDependencies(this);
    Traceur.enableLogging();

    RxJavaPlugins.setErrorHandler(e -> {
      e = Dank.errors().findActualCause(e);

      if (e instanceof IOException) {
        // Fine, file/network problem or API that throws on cancellation.
        Timber.w("IOException");
        return;
      }
      if (e instanceof InterruptedException || e.getCause() instanceof InterruptedIOException) {
        // Fine, some blocking code was interrupted by a dispose call.
        Timber.w("Interrupted exception");
        return;
      }
      if ((e instanceof NullPointerException) || (e instanceof IllegalArgumentException)) {
        // That's likely a bug in the application.
        Timber.w("Exception");
        Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        return;
      }
      if (e instanceof IllegalStateException) {
        // That's a bug in RxJava or in a custom operator.
        Timber.w("Exception");
        Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        return;
      }

      Timber.e(e, "Undeliverable exception received, not sure what to do.");
    });

    registerActivityLifecycleCallbacks(new SimpleActivityLifecycleCallbacks() {
      int activeActivitiesCount = 0;

      @Override
      public void onActivityStarted(Activity activity) {
        ++activeActivitiesCount;
      }

      @Override
      public void onActivityStopped(Activity activity) {
        --activeActivitiesCount;

        if (activeActivitiesCount == 0) {
          dankMinimizeRelay.accept(new Object());
        }
      }
    });
  }

  /**
   * An Observable that emits whenever all activities of the app are minimized/stopped.
   * Note 1: Unsubscribe only in onDestroy(). Unsubscribing in onStop() will be incorrect.
   * Note 2: Do not forget #1.
   */
  @CheckResult
  public static Relay<Object> appMinimizeStream() {
    return dankMinimizeRelay;
  }
}
