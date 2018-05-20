package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Submission;

import me.saket.dank.ui.UiEvent;
import me.saket.dank.utils.Optional;

@AutoValue
public abstract class SubmissionChanged implements UiEvent {

  public abstract Optional<Submission> optionalSubmission();

  public static SubmissionChanged create(Optional<Submission> submission) {
    return new AutoValue_SubmissionChanged(submission);
  }
}
