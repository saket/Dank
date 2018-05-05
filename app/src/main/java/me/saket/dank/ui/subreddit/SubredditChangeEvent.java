package me.saket.dank.ui.subreddit;

import com.google.auto.value.AutoValue;

import me.saket.dank.ui.UiEvent;

@AutoValue
public abstract class SubredditChangeEvent implements UiEvent {

  public abstract String subredditName();

  public static SubredditChangeEvent create(String subredditName) {
    return new AutoValue_SubredditChangeEvent(subredditName);
  }
}
