package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import me.saket.dank.ui.UiEvent;
import me.saket.dank.utils.DankSubmissionRequest;

@AutoValue
public abstract class SubmissionRequestChanged implements UiEvent {

  public abstract DankSubmissionRequest request();

  public static SubmissionRequestChanged create(DankSubmissionRequest request) {
    return new AutoValue_SubmissionRequestChanged(request);
  }
}
