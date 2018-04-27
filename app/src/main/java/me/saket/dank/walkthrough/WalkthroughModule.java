package me.saket.dank.walkthrough;

import android.app.Application;
import android.content.Context;
import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import javax.inject.Singleton;

@Module
public class WalkthroughModule {

  @Provides
  @Singleton
  @Named("walkthroughs")
  static RxSharedPreferences provideSharedPrefsForUserPrefs(Application appContext) {
    return RxSharedPreferences.create(appContext.getSharedPreferences("walkthroughs", Context.MODE_PRIVATE));
  }

  @Provides
  @Named("user_learned_submission_gestures")
  static Preference<Boolean> hasUserLearnedPref(@Named("user_prefs") RxSharedPreferences sharedPrefs) {
    return sharedPrefs.getBoolean("user_learned_submission_gestures", false);
  }
}
