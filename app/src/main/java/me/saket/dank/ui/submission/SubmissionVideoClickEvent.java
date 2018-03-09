package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SubmissionVideoClickEvent {

  public abstract long seekPosition();

  public static SubmissionVideoClickEvent create(long seekPosition) {
    return new AutoValue_SubmissionVideoClickEvent(seekPosition);
  }
}
