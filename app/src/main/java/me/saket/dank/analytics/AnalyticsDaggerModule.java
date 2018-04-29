package me.saket.dank.analytics;

import dagger.Module;
import dagger.Provides;

@Module
public class AnalyticsDaggerModule {

  @Provides
  static CrashReporter crashReporter(BugsnagCrashReporter bugsnag) {
    return bugsnag;
  }
}
