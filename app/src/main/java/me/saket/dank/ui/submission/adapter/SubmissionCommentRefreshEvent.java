package me.saket.dank.ui.submission.adapter;

import com.google.auto.value.AutoValue;

import me.saket.dank.ui.UiEvent;

@AutoValue
public abstract class SubmissionCommentRefreshEvent implements UiEvent {

  public static SubmissionCommentRefreshEvent create() {
    return new AutoValue_SubmissionCommentRefreshEvent();
  }
}
