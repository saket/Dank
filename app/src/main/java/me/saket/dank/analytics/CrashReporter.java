package me.saket.dank.analytics;

import timber.log.Timber;

public interface CrashReporter {

  Timber.Tree timberTree();

  void notify(Throwable throwable);
}
