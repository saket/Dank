package me.saket.dank;

import android.app.Application;

import com.jakewharton.threetenabp.AndroidThreeTen;
import com.tspoon.traceur.Traceur;

import java.io.IOException;
import java.io.InterruptedIOException;

import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.subreddits.SubredditSubscriptionsSyncJob;
import me.saket.dank.notifs.UnreadMessageSyncJob;
import timber.log.Timber;

public class DankApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        Dank.initDependencies(this);
        AndroidThreeTen.init(this);
        Traceur.enableLogging();

        // Sync jobs.
        SubredditSubscriptionsSyncJob.schedule(this);
        UnreadMessageSyncJob.schedule(this);

        RxJavaPlugins.setErrorHandler(e -> {
            if (e instanceof UndeliverableException) {
                e = e.getCause();
            }
            if (e instanceof IOException) {
                // Fine, irrelevant network problem or API that throws on cancellation.
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
    }

}
