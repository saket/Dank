package me.saket.dank;

import android.app.Application;

import me.saket.dank.di.Dank;
import timber.log.Timber;

public class DankApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Timber.
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        // Initialize Dagger graph.
        Dank.initDependencies(this);
    }

}
