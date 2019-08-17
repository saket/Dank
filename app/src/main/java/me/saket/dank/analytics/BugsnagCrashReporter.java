package me.saket.dank.analytics;

import android.app.Application;
import androidx.annotation.Nullable;
import android.util.Log;

import com.bugsnag.android.BeforeNotify;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Error;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class BugsnagCrashReporter implements CrashReporter, BeforeNotify {

  private final TimberTree tree;

  @Inject
  public BugsnagCrashReporter(Application appContext) {
    tree = new TimberTree();
    Bugsnag.init(appContext);
    Bugsnag.getClient().beforeNotify(this);
  }

  @Override
  public Timber.Tree timberTree() {
    return tree;
  }

  @Override
  public void notify(Throwable throwable) {
    Bugsnag.notify(throwable);
  }

  @Override
  public void identifyUser(@Nullable String user) {
    Bugsnag.setUserName(user);
  }

  @Override
  public boolean run(Error error) {
    tree.addBufferedLogs(error);
    return true;
  }

  /**
   * A logging implementation which buffers the last 200 messages and notifies on error exceptions.
   */
  public static class TimberTree extends Timber.Tree {
    private static final int BUFFER_SIZE = 200;

    // Adding one to the initial size accounts for the add before remove.
    private final Deque<String> buffer = new ArrayDeque<>(BUFFER_SIZE + 1);

    @Override
    protected void log(int priority, @Nullable String tag, String message, Throwable t) {
      message = System.currentTimeMillis() + " " + priorityToString(priority) + " " + message;
      synchronized (buffer) {
        buffer.addLast(message);
        if (buffer.size() > BUFFER_SIZE) {
          buffer.removeFirst();
        }
      }
      if (t != null && priority == Log.ERROR) {
        Bugsnag.notify(t);
      }
    }

    void addBufferedLogs(Error error) {
      synchronized (buffer) {
        int i = 1;
        for (String message : buffer) {
          error.addToTab("Log", String.format(Locale.US, "%03d", i++), message);
        }
      }
    }

    private static String priorityToString(int priority) {
      switch (priority) {
        case Log.ERROR:
          return "E";
        case Log.WARN:
          return "W";
        case Log.INFO:
          return "I";
        case Log.DEBUG:
          return "D";
        default:
          return String.valueOf(priority);
      }
    }
  }
}
