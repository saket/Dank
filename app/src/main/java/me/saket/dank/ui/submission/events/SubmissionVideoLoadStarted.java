package me.saket.dank.ui.submission.events;

import me.saket.dank.ui.UiEvent;

public class SubmissionVideoLoadStarted implements UiEvent {

  public static SubmissionVideoLoadStarted create() {
    return new SubmissionVideoLoadStarted();
  }
}
