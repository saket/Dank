package me.saket.dank.analytics;

import androidx.annotation.Nullable;

import timber.log.Timber;

public interface CrashReporter {

  Timber.Tree timberTree();

  void notify(Throwable throwable);

  void identifyUser(@Nullable String user);
}
