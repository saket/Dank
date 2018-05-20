package me.saket.dank.ui.submission.events;

import me.saket.dank.ui.UiEvent;

public class SubmissionImageLoadSucceeded implements UiEvent {

  public static SubmissionImageLoadSucceeded create() {
    return new SubmissionImageLoadSucceeded();
  }
}
