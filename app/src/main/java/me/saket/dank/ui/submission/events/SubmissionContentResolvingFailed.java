package me.saket.dank.ui.submission.events;

import me.saket.dank.ui.UiEvent;

public class SubmissionContentResolvingFailed implements UiEvent {

  public static SubmissionContentResolvingFailed create() {
    return new SubmissionContentResolvingFailed();
  }
}
