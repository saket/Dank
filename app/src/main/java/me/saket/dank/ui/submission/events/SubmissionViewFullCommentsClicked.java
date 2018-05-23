package me.saket.dank.ui.submission.events;

import me.saket.dank.ui.UiEvent;

public class SubmissionViewFullCommentsClicked implements UiEvent {

  public static SubmissionViewFullCommentsClicked create() {
    return new SubmissionViewFullCommentsClicked();
  }
}
