package me.saket.dank;

import android.app.Application;

import com.idescout.sql.SqlScoutServer;
import com.jakewharton.threetenabp.AndroidThreeTen;

import me.saket.dank.di.Dank;
import me.saket.dank.ui.subreddits.SubredditSubscriptionsSyncJob;
import timber.log.Timber;

public class DankApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
            SqlScoutServer.create(this, getPackageName());
        }

        Dank.initDependencies(this);
        AndroidThreeTen.init(this);
    }

}
