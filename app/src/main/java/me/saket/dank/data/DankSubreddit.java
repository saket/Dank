package me.saket.dank.data;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

/**
 * This class exists because Reddit returns the frontpage when an empty subreddit name is passed to it.
 * /r/Frontpage is an entirely different subreddit.
 */
@AutoValue
public abstract class DankSubreddit implements Parcelable {

  public abstract String displayName();

  /**
   * This will always be the same as {@link #displayName()}, except for the frontpage where
   * this will be <var>null</var> whereas {@link #displayName()} will return "Frontpage".
   */
  @Nullable
  public abstract String name();

  public static DankSubreddit create(String displayName) {
    return new AutoValue_DankSubreddit(displayName, displayName);
  }

  public static DankSubreddit createFrontpage(String displayName) {
    return new AutoValue_DankSubreddit(displayName, null);
  }

  public boolean isFrontpage() {
    return name() == null;
  }

}
