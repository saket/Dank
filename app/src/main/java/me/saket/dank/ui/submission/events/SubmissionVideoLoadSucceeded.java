package me.saket.dank.ui.submission.events;

import me.saket.dank.ui.UiEvent;

public class SubmissionVideoLoadSucceeded implements UiEvent {

  public static SubmissionVideoLoadSucceeded create() {
    return new SubmissionVideoLoadSucceeded();
  }
}
