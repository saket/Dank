package me.saket.dank.di;

import com.squareup.moshi.Moshi;

import javax.inject.Singleton;

import dagger.Component;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.SharedPrefsManager;

@Component(modules = DankAppModule.class)
@Singleton
public interface DankAppComponent {

    DankRedditClient dankRedditClient();

    SharedPrefsManager sharedPrefs();

    Moshi moshi();

}
