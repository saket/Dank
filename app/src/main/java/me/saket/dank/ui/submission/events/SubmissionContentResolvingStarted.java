package me.saket.dank.ui.submission.events;

import me.saket.dank.ui.UiEvent;

public class SubmissionContentResolvingStarted implements UiEvent {

  public static SubmissionContentResolvingStarted create() {
    return new SubmissionContentResolvingStarted();
  }
}
