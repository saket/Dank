package me.saket.dank.ui.submission.events;

import me.saket.dank.ui.UiEvent;

public class SubmissionNsfwContentFiltered implements UiEvent {

  public static SubmissionNsfwContentFiltered create() {
    return new SubmissionNsfwContentFiltered();
  }
}
