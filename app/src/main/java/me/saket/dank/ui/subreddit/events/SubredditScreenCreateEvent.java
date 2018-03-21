package me.saket.dank.ui.subreddit.events;

import me.saket.dank.ui.UiEvent;

public class SubredditScreenCreateEvent implements UiEvent {

  public static SubredditScreenCreateEvent create() {
    return new SubredditScreenCreateEvent();
  }
}
