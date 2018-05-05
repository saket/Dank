package me.saket.dank.walkthrough;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class WalkthroughModule {

  @Provides
  @Named("walkthroughs")
  static SharedPreferences provideSharedPrefsForUserPrefs(Application appContext) {
    return appContext.getSharedPreferences("walkthroughs", Context.MODE_PRIVATE);
  }

  @Provides
  @Singleton
  @Named("walkthroughs")
  static RxSharedPreferences provideRxSharedPrefsForUserPrefs(@Named("walkthroughs") SharedPreferences sharedPrefs) {
    return RxSharedPreferences.create(sharedPrefs);
  }

  @Provides
  @Named("user_learned_submission_gestures")
  static Preference<Boolean> hasUserLearnedSubmissionGesturesPref(@Named("walkthroughs") RxSharedPreferences rxPrefs) {
    return rxPrefs.getBoolean("submission_gestures_learned_2", false);
  }

  @Provides
  @Named("welcome_text_shown")
  static Preference<Boolean> welcomePref(@Named("walkthroughs") RxSharedPreferences rxPrefs) {
    return rxPrefs.getBoolean("welcome_text_shown", false);
  }
}
