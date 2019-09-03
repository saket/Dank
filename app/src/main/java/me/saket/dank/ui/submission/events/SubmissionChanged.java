package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import me.saket.dank.ui.UiEvent;
import me.saket.dank.ui.submission.SubmissionAndComments;
import me.saket.dank.utils.Optional;

@AutoValue
public abstract class SubmissionChanged implements UiEvent {

  public abstract Optional<SubmissionAndComments> optionalSubmission();

  public static SubmissionChanged create(Optional<SubmissionAndComments> submission) {
    return new AutoValue_SubmissionChanged(submission);
  }
}
