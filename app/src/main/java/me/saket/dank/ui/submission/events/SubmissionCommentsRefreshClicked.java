package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import me.saket.dank.ui.UiEvent;

@AutoValue
public abstract class SubmissionCommentsRefreshClicked implements UiEvent {

  public static SubmissionCommentsRefreshClicked create() {
    return new AutoValue_SubmissionCommentsRefreshClicked();
  }
}
